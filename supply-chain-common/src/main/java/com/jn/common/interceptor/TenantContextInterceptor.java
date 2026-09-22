package com.jn.common.interceptor;

import com.jn.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 解析网关透传的基础上下文 (核心修复点！)
        String tenantIdStr = request.getHeader("X-Tenant-Id");
        String userIdStr = request.getHeader("X-User-Id");
        String deptIdStr = request.getHeader("X-Dept-Id");

        if (StringUtils.hasText(tenantIdStr)) UserContext.setTenantId(Long.parseLong(tenantIdStr));
        if (StringUtils.hasText(userIdStr)) UserContext.setUserId(Long.parseLong(userIdStr));
        if (StringUtils.hasText(deptIdStr)) UserContext.setDeptId(Long.parseLong(deptIdStr));

        // 2. 解析超管跨租户的目标上下文
        String targetTenantStr = request.getHeader("X-Target-Tenant-Id");
        if (StringUtils.hasText(targetTenantStr) && UserContext.isSuperAdmin()) {
            try {
                UserContext.setTargetTenantId(Long.parseLong(targetTenantStr));
                log.debug("超管跨租户查询，临时切换 tenantId 为: {}", targetTenantStr);
            } catch (NumberFormatException e) {
                log.warn("无效的 X-Target-Tenant-Id: {}", targetTenantStr);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 【极其重要】：请求结束后必须清理 ThreadLocal，防止 Tomcat 线程池复用导致数据串号！
        UserContext.clear();
    }
}


