package com.jn.trade.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jn.common.Result;
import com.jn.common.context.UserContext;
import com.jn.trade.entity.BizContract;
import com.jn.trade.service.BizCategoryService;
import com.jn.trade.service.BizContractService;
import com.jn.trade.service.BizTenantCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/business/contract")
@RequiredArgsConstructor
public class ContractController {

    private final BizContractService contractService;

    private final BizTenantCategoryService tenantCategoryService;

    private final BizCategoryService categoryService;

    /**
     * 创建合同 (触发 Seata 分布式事务)
     */
    @PostMapping
    public Long createContract(@RequestBody Map<String, Object> payload) {
        Long categoryId = Long.valueOf(payload.get("categoryId").toString());
        Long supplierId = Long.valueOf(payload.get("supplierId").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> dynamicFields = (Map<String, Object>) payload.get("dynamicFields");

        return contractService.createContract(categoryId, supplierId, dynamicFields);
    }

    /**
     * 提交审批 (触发状态机)
     */
    @PostMapping("/{id}/submit")
    public void submit(@PathVariable Long id) {
        contractService.submitForApproval(id);
    }

    /**
     * 审批通过 (触发状态机)
     */
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        contractService.approveContract(id);
    }

    /**
     * 分页查询合同
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('trade:contract:list') or hasAuthority('*:*:*')")
    public Result<Page<BizContract>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            // 【新增】：超管可以传入目标租户ID进行筛选
            @RequestParam(required = false) Long targetTenantId) {



        Page<BizContract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<BizContract>()
                .like(StringUtils.hasText(contractNo), BizContract::getContractNo, contractNo)
                .eq(categoryId != null, BizContract::getCategoryId, categoryId)
                .eq(status != null, BizContract::getStatus, status)
                .orderByDesc(BizContract::getCreateTime);

        // 【核心逻辑】：处理超管跨租户查询
        Long currentTenantId = UserContext.getTenantId();
        if (currentTenantId == 0L) { // 如果是超级管理员
            if (targetTenantId != null && targetTenantId != 0L) {
                // 超管指定了查看某个租户，手动拼接条件 (此时多租户插件通常会对 tenant_id=0 放行)
                wrapper.eq(BizContract::getTenantId, targetTenantId);
            }
            // 如果超管没指定 targetTenantId，则查看所有租户的合同 (多租户插件对 tenant_id=0 默认不拼接条件)
        }
        return Result.success(contractService.page(page, wrapper));
    }

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:contract:query') or hasAuthority('*:*:*')")
    public Result<BizContract> detail(@PathVariable Long id) {
        return Result.success(contractService.getById(id));
    }

    /**
     * 审核/更新合同状态
     */
    @PutMapping("/audit")
    @PreAuthorize("hasAuthority('trade:contract:audit') or hasAuthority('*:*:*')")
    public Result<Void> audit(@RequestBody BizContract contract) {
        // 仅更新状态和可能存在的备注字段，防止全量更新覆盖其他数据
        BizContract updateEntity = new BizContract();
        updateEntity.setId(contract.getId());
        updateEntity.setStatus(contract.getStatus());
        contractService.updateById(updateEntity);
        return Result.success();
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:contract:remove') or hasAuthority('*:*:*')")
    public Result<Void> remove(@PathVariable Long id) {
        contractService.removeById(id);
        return Result.success();
    }
}

