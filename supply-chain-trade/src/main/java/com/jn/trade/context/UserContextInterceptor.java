package com.jn.trade.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从网关透传的 Header 中获取用户信息
        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");
        String deptId = request.getHeader("X-Dept-Id");
        String roles = request.getHeader("X-Roles"); // 假设网关也透传了角色

        if (StringUtils.hasText(tenantId)) UserContext.setTenantId(Long.parseLong(tenantId));
        if (StringUtils.hasText(userId)) UserContext.setUserId(Long.parseLong(userId));
        if (StringUtils.hasText(deptId)) UserContext.setDeptId(Long.parseLong(deptId));

        // 简单判断是否为管理员（实际可根据 roles 解析）
        if (StringUtils.hasText(roles) && roles.contains("ROLE_ADMIN")) {
            UserContext.setIsManager(true);
        } else {
            UserContext.setIsManager(false);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后必须清理，防止内存泄漏和线程池复用导致的数据错乱
        UserContext.clear();
    }
}

