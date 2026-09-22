package com.jn.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 开启 @PreAuthorize 方法级权限校验
public class SecurityConfig {

    // 默认安全过滤链 (处理用户登录、API 保护)
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 放行自定义登录接口、错误页、Swagger等
                        .requestMatchers(
                                "/admin/system/auth/login",
                                "/admin/system/auth/register",
                                "/admin/system/auth/logout",
                                "/admin/system/auth/captcha",
                                "/app/auth/**", // C端登录也放行
                                "/error",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 【关键修改】：应用自定义的 JWT 权限转换器
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // 【关键修改】：禁用默认的表单登录，防止拦截 /login 请求
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 微服务接口禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 【核心新增】：自定义 JWT 权限转换器
     * 告诉 Spring Security 从 JWT 的 "roles" claim 中提取权限列表
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 1. 从 JWT 中获取我们自定义的 "roles" 字段 (包含 ROLE_ADMIN, sys:role:list 等)
            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 将字符串列表转换为 Spring Security 需要的 GrantedAuthority 集合
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

