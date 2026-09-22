package com.jn.trade.controller.app;

import com.jn.common.Result;
import com.jn.common.context.UserContext;
import com.jn.trade.entity.BizCategory;
import com.jn.trade.entity.BizContract;
import com.jn.trade.service.BizCategoryService;
import com.jn.trade.service.BizContractService;
import com.jn.trade.service.BizTenantCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/app/business/contract")
@RequiredArgsConstructor
public class BizContractController {

    private final BizContractService contractService;
    private final BizTenantCategoryService tenantCategoryService;
    private final BizCategoryService categoryService;

    /**
     * C端用户获取自己租户可用的品类列表 (包含品类名称和ID)
     */
    @GetMapping("/my-categories")
    public Result<List<BizCategory>> getMyCategories() {
        Long tenantId = UserContext.getTenantId();
        if (tenantId == null) {
            return Result.error(401, "用户未登录或租户信息丢失");
        }

        // 1. 获取该租户关联的品类ID
        List<Long> categoryIds = tenantCategoryService.getCategoryIdsByTenant(tenantId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 2. 根据ID批量查询品类详情
        List<BizCategory> categories = categoryService.listByIds(categoryIds);
        return Result.success(categories);
    }

    // C端用户提交合同
    @PostMapping
    public Result<Void> create(@RequestBody BizContract contract) {
        contract.setTenantId(UserContext.getTenantId());
        contract.setCreateBy(UserContext.getUserId());
        contractService.save(contract);
        return Result.success();
    }
}
