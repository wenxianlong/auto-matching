package com.jn.user.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.jn.user.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // 1. OAuth2 协议端点安全过滤链 (优先级最高)
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults()); // 启用 OpenID Connect 1.0

        // 未认证时重定向到登录页
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/auth/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );

        return http.build();
    }

    // 2. 注册 OAuth2 客户端 (这里使用内存配置，生产环境建议存入数据库)
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // 配置一个名为 supply-chain-app 的客户端
        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("supply-chain-app")
                .clientSecret("{noop}secret123") // 生产环境应使用 {bcrypt} 加密
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD) // 支持密码模式(需自定义扩展，此处先用标准模式)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(oidcClient);
    }

    // 3. 自定义 JWT Token 内容 (将 tenantId, deptId, roles 注入 Token)
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(CustomUserDetailsService userDetailsService) {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                // 获取当前认证的用户信息
                CustomUserDetailsService.LoginUser loginUser = (CustomUserDetailsService.LoginUser) context.getPrincipal().getPrincipal();

                // 将自定义字段放入 JWT Claims
                context.getClaims().claim("tenant_id", loginUser.getTenantId());
                context.getClaims().claim("dept_id", loginUser.getDeptId());
                context.getClaims().claim("user_id", loginUser.getUserId());
                context.getClaims().claim("roles", loginUser.getAuthorities().stream()
                        .map(Object::toString).toList());
            }
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }


    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // 1. 获取当前 JVM 的工作目录
        String basePath = System.getProperty("user.dir");
        log.info("当前 JVM 工作目录 (user.dir): {}", basePath);

        // 2. 智能判断：如果工作目录是父工程，则自动追加子模块路径
        // (请确保 "supply-chain-user" 与您实际的子模块文件夹名称一致)
        if (!basePath.endsWith("supply-chain-user") && !basePath.endsWith("user-service")) {
            File subModule = new File(basePath, "supply-chain-user");
            if (subModule.exists() && subModule.isDirectory()) {
                basePath = subModule.getAbsolutePath();
                log.info("检测到父工程目录，自动切换到子模块目录: {}", basePath);
            }
        }

        // 3. 定义密钥存放目录
        File keysDir = new File(basePath, "keys");
        if (!keysDir.exists()) {
            boolean created = keysDir.mkdirs();
            log.info("创建 keys 目录结果: {}, 绝对路径: {}", created, keysDir.getAbsolutePath());
        } else {
            log.info("keys 目录已存在，绝对路径: {}", keysDir.getAbsolutePath());
        }

        File privateKeyFile = new File(keysDir, "private.key");
        File publicKeyFile = new File(keysDir, "public.key");

        KeyPair keyPair;
        try {
            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                log.info("未找到 RSA 密钥文件，正在自动生成...");
                keyPair = generateRsaKey();
                writePem(keyPair.getPrivate(), privateKeyFile, "PRIVATE");
                writePem(keyPair.getPublic(), publicKeyFile, "PUBLIC");
                log.info("✅ RSA 密钥对生成成功！");
                log.info("🔑 私钥路径: {}", privateKeyFile.getAbsolutePath());
                log.info("🔑 公钥路径: {}", publicKeyFile.getAbsolutePath());
                log.info("⚠️ 【重要】请将 public.key 的内容复制到网关 (gateway) 的 application.yml 中！");
            } else {
                log.info("从本地文件加载 RSA 密钥对...");
                keyPair = readKeyPair(privateKeyFile, publicKeyFile);
                log.info("✅ RSA 密钥对加载成功！");
            }
        } catch (Exception e) {
            log.error("❌ RSA 密钥加载或生成失败", e);
            throw new RuntimeException("RSA 密钥初始化失败", e);
        }

        // ... 后面的构建 JWK 对象代码保持不变 ...
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * 配置 JWT 编码器 (用于生成和签名 JWT Token)
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    // ================= 密钥生成与读写工具方法 =================

    private static KeyPair generateRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048); // 使用 2048 位长度
        return gen.generateKeyPair();
    }

    private static void writePem(Key key, File file, String type) throws IOException {
        // 使用 MimeEncoder 确保 Base64 字符串按标准 PEM 格式每 64 个字符换行
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
        String pem = "-----BEGIN " + type + " KEY-----\n" + base64 + "\n-----END " + type + " KEY-----\n";
        Files.writeString(file.toPath(), pem);
    }

    private static KeyPair readKeyPair(File privFile, File pubFile) throws Exception {
        String privPem = new String(Files.readAllBytes(privFile.toPath()));
        String pubPem = new String(Files.readAllBytes(pubFile.toPath()));

        PrivateKey privateKey = getPrivateKey(privPem);
        PublicKey publicKey = getPublicKey(pubPem);
        return new KeyPair(publicKey, privateKey);
    }

    private static PrivateKey getPrivateKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(cleanPem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey getPublicKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(cleanPem);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }


}
