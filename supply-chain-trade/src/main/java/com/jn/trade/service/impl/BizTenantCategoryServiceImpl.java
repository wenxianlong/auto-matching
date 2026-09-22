package com.jn.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.trade.entity.BizTenantCategory;
import com.jn.trade.mapper.BizTenantCategoryMapper;
import com.jn.trade.service.BizTenantCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BizTenantCategoryServiceImpl extends ServiceImpl<BizTenantCategoryMapper, BizTenantCategory> implements BizTenantCategoryService {

    @Override
    public List<Long> getCategoryIdsByTenant(Long tenantId) {
        List<BizTenantCategory> list = this.list(new LambdaQueryWrapper<BizTenantCategory>()
                .eq(BizTenantCategory::getTenantId, tenantId));
        return list.stream().map(BizTenantCategory::getCategoryId).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveTenantCategories(Long tenantId, List<Long> categoryIds) {
        // 1. 物理删除该租户原有的所有品类关联
        this.remove(new LambdaQueryWrapper<BizTenantCategory>()
                .eq(BizTenantCategory::getTenantId, tenantId));

        // 2. 批量插入新的关联关系
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<BizTenantCategory> entities = new ArrayList<>();
            for (Long categoryId : categoryIds) {
                BizTenantCategory entity = new BizTenantCategory();
                entity.setTenantId(tenantId);
                entity.setCategoryId(categoryId);
                entities.add(entity);
            }
            this.saveBatch(entities);
        }
    }
}

