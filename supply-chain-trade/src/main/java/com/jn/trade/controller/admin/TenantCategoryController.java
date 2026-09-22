package com.jn.trade.controller.admin;

import com.jn.common.Result;
import com.jn.trade.service.BizTenantCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/business/tenant-category")
@RequiredArgsConstructor
public class TenantCategoryController {

    private final BizTenantCategoryService tenantCategoryService;

    /**
     * 获取指定租户已开通的品类ID列表
     */
    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('trade:tenant:config') or hasAuthority('*:*:*')")
    public Result<List<Long>> getTenantCategories(@PathVariable(name = "tenantId") Long tenantId) {
        return Result.success(tenantCategoryService.getCategoryIdsByTenant(tenantId));
    }

    /**
     * 保存租户的品类授权 (全量覆盖：传入最新的品类ID列表)
     * @param tenantId 目标租户ID
     * @param categoryIds 选中的品类ID列表
     */
    @PostMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('trade:tenant:config') or hasAuthority('*:*:*')")
    public Result<Void> saveTenantCategories(
            @PathVariable(name = "tenantId") Long tenantId,
            @RequestBody List<Long> categoryIds) {
        tenantCategoryService.saveTenantCategories(tenantId, categoryIds);
        return Result.success();
    }
}
