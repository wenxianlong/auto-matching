package com.jn.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.trade.entity.BizTenantCategory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BizTenantCategoryService extends IService<BizTenantCategory> {
    /**
     * 获取指定租户已开通的品类ID列表
     */
    List<Long> getCategoryIdsByTenant(Long tenantId);

    @Transactional(rollbackFor = Exception.class)
    void saveTenantCategories(Long tenantId, List<Long> categoryIds);
}
