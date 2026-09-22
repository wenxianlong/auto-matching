package com.jn.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate reactiveRedisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final GatewayAuthProperties authProperties;

    private PublicKey publicKey;

    /**
     * 初始化 RSA 公钥
     */
    @PostConstruct
    public void init() {
        try {
            // 去除 PEM 格式的头尾和换行符、空格
            String cleanKey = authProperties.getPublicKey()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(keySpec);
            log.info("网关 RSA 公钥加载成功");
        } catch (Exception e) {
            log.error("网关 RSA 公钥加载失败，请检查 application.yml 中的 gateway.auth.public-key 配置！", e);
            throw new RuntimeException("网关 RSA 公钥加载失败", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 放行 OPTIONS 跨域预检请求
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 3. 获取并校验 Token 格式
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证 Token，请先登录");
        }
        String token = authHeader.substring(7);

        // 4. 解析并校验 JWT 签名 (RSA)
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, "Token 已过期，请重新登录");
        } catch (SignatureException | MalformedJwtException e) {
            return unauthorized(exchange, "Token 签名无效或格式错误");
        } catch (Exception e) {
            log.error("JWT 解析异常", e);
            return unauthorized(exchange, "Token 解析失败");
        }

        // 5. 提取用户上下文信息
        Long userId = claims.get("user_id", Long.class);
        Long tenantId = claims.get("tenant_id", Long.class);
        Long deptId = claims.get("dept_id", Long.class);
        Object roles = claims.get("roles");

        // 【关键修复】：显式声明为 final long，确保在后续的 lambda 表达式中绝对可用
        final long iat = claims.getIssuedAt().toInstant().getEpochSecond();

        // 6. 【核心】：结合 Redis 校验 Token 是否被主动失效 (如：修改密码、被踢下线)
        String redisKey = "user:token:valid_time:" + userId;

        return reactiveRedisTemplate.opsForValue().get(redisKey)
                // 【关键修复】：安全解析字符串为 Long，防止 Redis 中存在脏数据导致 NumberFormatException 中断响应式流
                .map(timeStr -> {
                    try {
                        return (timeStr != null && !timeStr.trim().isEmpty()) ? Long.parseLong(timeStr.trim()) : 0L;
                    } catch (NumberFormatException e) {
                        log.warn("Redis 中的失效时间戳格式错误, key: {}, value: {}", redisKey, timeStr);
                        return 0L; // 解析失败时兜底返回 0L，保证流继续向下走
                    }
                })
                .defaultIfEmpty(0L) // 如果 Redis 中完全没有这个 key (Empty Mono)，默认返回 0L (代表未被踢下线)
                .flatMap(validTime -> {
                    // 如果 JWT 的签发时间 早于 Redis 中记录的失效时间，说明 Token 已作废
                    if (iat < validTime) {
                        return unauthorized(exchange, "登录状态已失效，请重新登录");
                    }

                    // 7. 校验通过，构建透传给下游微服务的 Header
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-Tenant-Id", String.valueOf(tenantId != null ? tenantId : 0))
                            .header("X-Dept-Id", String.valueOf(deptId != null ? deptId : 0))
                            .header("X-Roles", roles != null ? roles.toString() : "")
                            // 保留原始 Authorization 头，以便下游微服务的 OAuth2 ResourceServer 进行二次校验（可选）
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .build();

                    boolean isAdminRequest = path.startsWith("/admin/");
                    // 构建透传给下游微服务的 Header
                    ServerHttpRequest.Builder mutatedRequestBuilder = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header(HttpHeaders.AUTHORIZATION, authHeader);

                    if (isAdminRequest) {
                        // 【B端管理端】：严格透传租户、部门、角色信息，下游需要用于 RBAC 和多租户拦截
                        mutatedRequestBuilder
                                .header("X-Tenant-Id", String.valueOf(tenantId != null ? tenantId : 0))
                                .header("X-Dept-Id", String.valueOf(deptId != null ? deptId : 0))
                                .header("X-Roles", roles != null ? roles.toString() : "");
                    } else {
                        // 【C端服务端】：只透传 User-Id，C端不需要复杂的租户和 RBAC 概念
                        // (如果 C端也有租户概念，可以透传 X-Tenant-Id，但通常 C端用户不感知租户)
                        log.debug("C端请求，仅透传 X-User-Id");
                    }

                    // 8. 放行请求
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    /**
     * 判断是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        if (authProperties.getWhiteList() == null || authProperties.getWhiteList().isEmpty()) {
            return false;
        }
        // 【新增调试日志 1】：打印当前内存中加载的白名单列表！
        log.info("=== 白名单调试 === 当前内存中加载的白名单列表: {}", authProperties.getWhiteList());

        if (authProperties.getWhiteList() == null || authProperties.getWhiteList().isEmpty()) {
            log.error("=== 白名单调试 === 致命错误：白名单列表为空！请检查 application.yml 或 Nacos 配置！");
            return false;
        }
        boolean isMatch = authProperties.getWhiteList().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        // 【新增】：打印白名单匹配结果，方便调试
        log.info("=== 白名单校验 === 请求路径: {}, 匹配结果: {}", path, isMatch);
        return  isMatch;
    }

//    if (authProperties.getWhiteList() == null || authProperties.getWhiteList().isEmpty()) {
//        return false;
//    }
//
//    // 【核心优化】：兼容处理。
//    // 如果请求路径以 /api 开头，生成一个去掉 /api 的备用路径
//    String pathWithoutApi = path.startsWith("/api") ? path.substring(4) : path;
//
//    // 只要 原路径 或 去掉/api的路径 有一个匹配成功，就放行
//        return authProperties.getWhiteList().stream().anyMatch(pattern ->
//            pathMatcher.match(pattern, path) || pathMatcher.match(pattern, pathWithoutApi)
//            );

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String result = "{\"code\":401,\"msg\":\"" + msg + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(result.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 优先级尽量高，确保在路由转发前执行
        return -100;
    }
}



