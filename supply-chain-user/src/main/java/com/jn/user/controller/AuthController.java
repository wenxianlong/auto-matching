package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysUser;
import com.jn.user.service.CustomUserDetailsService;
import com.jn.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/system")
public class AuthController { // 如果已有此类，直接合并方法

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    private final PasswordEncoder passwordEncoder;

    private final SysUserService sysUserService;

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 1. 校验账号密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        CustomUserDetailsService.LoginUser loginUser = (CustomUserDetailsService.LoginUser) authentication.getPrincipal();

        // 2. 构建 JWT Claims
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("supply-chain-system")
                .issuedAt(now)
                .expiresAt(now.plus(2, ChronoUnit.HOURS))
                .subject(username)
                .claim("user_id", loginUser.getUserId())
                .claim("tenant_id", loginUser.getTenantId())
                .claim("dept_id", loginUser.getDeptId())
                .claim("roles", loginUser.getAuthorities().stream().map(Object::toString).collect(Collectors.toList()))
                .build();

        // 3. 生成 Token
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("token_type", "Bearer");
        result.put("expires_in", 7200);
        return Result.success(result);
    }

    /**
     * 开发测试用：重置指定用户的密码
     * 调用示例：POST /auth/reset-password?username=admin&newPassword=123456
     */
    @PostMapping("/auth/reset-password")
    public String resetPassword(@RequestParam String username, @RequestParam String newPassword) {
        SysUser user = sysUserService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (user == null) return "用户不存在";

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserService.updateById(user);
        return "密码重置成功，新密码为: " + newPassword;
    }

}

