package com.jn.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.BusinessException;
import com.jn.common.context.UserContext;
import com.jn.user.entity.SysUser;
import com.jn.user.mapper.SysUserMapper;
import com.jn.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 强制让用户的旧 Token 失效
     */
    public void invalidateUserToken(Long userId) {
        String key = "user:token:valid_time:" + userId;
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // 【修改点】：使用 StringRedisTemplate 写入，确保在 Redis 中存的是纯数字字符串，与网关读取端完美契合
        stringRedisTemplate.opsForValue().set(key, String.valueOf(currentTimeSeconds), 2, TimeUnit.HOURS);
    }

    @Override
    public void createUser(SysUser user) {
        // 1. 校验用户名是否已存在
        long count = this.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 密码加密 (核心！)
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 如果前端没传密码，给个默认密码 123456
            user.setPassword(passwordEncoder.encode("123456"));
        }

        // 3. 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        // ================= 核心修复：自动注入多租户和部门信息 =================
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication instanceof JwtAuthenticationToken) {
//            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
//
//            // 从 JWT 中获取当前登录用户的 tenant_id
//            Long tenantId = jwt.getClaim("tenant_id");
//            if (tenantId != null) {
//                user.setTenantId(tenantId);
//            } else {
//                // 兜底：如果 JWT 中没有，给个默认值（防止数据库 NOT NULL 报错）
//                user.setTenantId(1L);
//            }
//
//            // 从 JWT 中获取当前登录用户的 dept_id
//            Long deptId = jwt.getClaim("dept_id");
//            if (deptId != null && user.getDeptId() == null) {
//                user.setDeptId(deptId);
//            } else if (user.getDeptId() == null) {
//                user.setDeptId(1L); // 兜底默认部门
//            }
//        } else {
//            // 如果是系统内部调用（没有登录态），给默认值
//            if (user.getTenantId() == null) user.setTenantId(1L);
//            if (user.getDeptId() == null) user.setDeptId(1L);
//        }
//        // =====================================================================
//
//        // 【修改点】：优先使用前端传入的 tenantId，如果没传，再使用当前登录用户的 tenantId 兜底
//        if (user.getTenantId() == null) {
//            if (authentication instanceof JwtAuthenticationToken) {
//                Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
//                Long tenantId = jwt.getClaim("tenant_id");
//                user.setTenantId(tenantId != null ? tenantId : 1L);
//            } else {
//                user.setTenantId(1L);
//            }
//        }
//
//        // 部门ID同理
//        if (user.getDeptId() == null) {
//            user.setDeptId(1L); // 简化处理，实际可根据租户查询默认部门
//        }
//        invalidateUserToken(user.getId());
        this.save(user);
    }

    @Override
    public void updateUser(SysUser user) {
        // 【工程化补丁】：防水平越权校验
        SysUser existingUser = this.getById(user.getId());
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }
        // 如果当前用户不是超管，且目标用户的 tenantId 与当前登录用户的 tenantId 不一致，拒绝操作
        Long currentTenantId = UserContext.getTenantId();
        if (!UserContext.isSuperAdmin() && !existingUser.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作其他租户的用户");
        }

        // 修改用户时，如果传了密码则重新加密，没传则不修改密码
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // MyBatis-Plus 更新时忽略 null 字段
        }
        // 禁止修改租户ID和核心标识
        user.setTenantId(null);
        user.setUsername(null);
        // 每次更新用户，版本号 +1
        //user.setVersion(existingUser.getVersion() + 1);
        invalidateUserToken(user.getId());
        this.updateById(user);
    }
}

