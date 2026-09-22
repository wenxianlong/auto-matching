package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysDept;
import com.jn.user.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:dept:list') or hasAuthority('*:*:*')")
    public Result<List<SysDept>> list(@RequestParam Long tenantId) {
        // 支持超管按租户查询，普通管理员由多租户拦截器自动过滤
        List<SysDept> depts = deptService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                        .eq(tenantId != null, SysDept::getTenantId, tenantId)
                        .orderByAsc(SysDept::getSortOrder)
        );
        return Result.success(depts);
    }

    @GetMapping("/tree")
    public Result<List<SysDept>> tree(@RequestParam(required = false) Long tenantId) {
        List<SysDept> depts = deptService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                        .eq(tenantId != null, SysDept::getTenantId, tenantId)
                        .eq(SysDept::getStatus, 1)
                        .orderByAsc(SysDept::getSortOrder)
        );
        return Result.success(deptService.buildDeptTree(depts));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:dept:add') or hasAuthority('*:*:*')")
    public Result<Void> add(@RequestBody SysDept dept) {
        deptService.createDept(dept);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:dept:edit') or hasAuthority('*:*:*')")
    public Result<Void> edit(@RequestBody SysDept dept) {
        deptService.updateDept(dept);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:dept:remove') or hasAuthority('*:*:*')")
    public Result<Void> remove(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.success();
    }
}
