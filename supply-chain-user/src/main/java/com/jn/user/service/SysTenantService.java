package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysTenant;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SysTenantService extends IService<SysTenant> {
    /**
     * 创建租户并初始化默认数据
     */
    void createTenant(SysTenant tenant);

    /**
     * 获取当前用户有权限查看的租户列表（防越权）
     */
    List<SysTenant> getAccessibleTenants();

    @Transactional(rollbackFor = Exception.class)
    boolean safeRemoveTenant(Long tenantId);
}

