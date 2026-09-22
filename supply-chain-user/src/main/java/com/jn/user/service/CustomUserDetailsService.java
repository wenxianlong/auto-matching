package com.jn.user.service;

import com.jn.user.entity.SysUser;
import com.jn.user.mapper.SysMenuMapper;
import com.jn.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = userMapper.loginByUsername(username);

        if (sysUser == null) throw new UsernameNotFoundException("用户不存在: " + username);
        if (sysUser.getStatus() == 0) throw new RuntimeException("用户已被停用");

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. 加载角色 (以 ROLE_ 开头)
        List<String> roleCodes = userMapper.loginSelectRoleCodesByUserId(sysUser.getId());
        authorities.addAll(roleCodes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

        // 2. 加载具体权限标识 (如 sys:user:add)
        List<String> perms = menuMapper.loginSelectPermsByUserId(sysUser.getId());
        authorities.addAll(perms.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

        return new LoginUser(sysUser.getId(), sysUser.getTenantId(), sysUser.getDeptId(),
                sysUser.getUsername(), sysUser.getPassword(), authorities);
    }

    public static class LoginUser extends org.springframework.security.core.userdetails.User {
        private final Long userId;
        private final Long tenantId;
        private final Long deptId;

        public LoginUser(Long userId, Long tenantId, Long deptId, String username, String password, List<SimpleGrantedAuthority> authorities) {
            super(username, password, authorities);
            this.userId = userId;
            this.tenantId = tenantId;
            this.deptId = deptId;
        }
        public Long getUserId() { return userId; }
        public Long getTenantId() { return tenantId; }
        public Long getDeptId() { return deptId; }
    }
}

