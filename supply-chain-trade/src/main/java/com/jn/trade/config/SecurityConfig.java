package com.jn.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 【核心修复 1】：放行 Druid 监控面板及其所有静态资源 (css, js 等)
                        // 必须放在最前面！
                        .requestMatchers("/druid/**").permitAll()

                        .requestMatchers("/actuator/**", "/v3/api-docs/**").permitAll()

                        // 【核心修复 2】：放行 Spring Boot 默认的错误处理路径
                        .requestMatchers("/error").permitAll()

                        // 放行您的登录等公开接口
                        .requestMatchers("/admin/system/auth/login", "/admin/system/auth/register").permitAll()

                        // 其他所有请求都需要 JWT 认证
                        .anyRequest().authenticated()
                )
                // 启用 OAuth2 资源服务器 (JWT 校验)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}


