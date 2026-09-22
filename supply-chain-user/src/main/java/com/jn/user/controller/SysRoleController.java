package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysRole;
import com.jn.user.mapper.SysRoleMapper;
import com.jn.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final SysRoleMapper roleMapper;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:role:list') or hasAuthority('*:*:*')")
    public Result<List<SysRole>> list(@RequestParam(required = false) Long tenantId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // 如果传了 tenantId，则查询指定租户的角色
        // (注：普通管理员查询时，多租户拦截器会自动追加他自己 tenant_id 的条件，保证安全)
        if (tenantId != null) {
            wrapper.eq(SysRole::getTenantId, tenantId);
        }

        return Result.success(roleService.list(wrapper));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:role:add')")
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:role:edit')")
    public Result<Void> edit(@RequestBody SysRole role) {
        roleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }

    /**
     * 【GET】获取角色已分配的菜单ID列表 (供前端 Tree 组件回显勾选状态)
     */
    @GetMapping("/{roleId}/{tenantId}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable(name = "roleId") Long roleId,
                                           @PathVariable(name = "tenantId") Long tenantId ) {
        List<Long> menuIds = roleMapper.selectMenuIdsByRoleIdAndTenantId(roleId,tenantId);
        return Result.success(menuIds);
    }

    /**
     * 【POST】给角色分配菜单权限
     */
    @PostMapping("/{roleId}/{tenantId}/menus")
    @PreAuthorize("hasAuthority('sys:role:edit')")
    public Result<Void> assignMenus(@PathVariable(name = "roleId") Long roleId, @PathVariable(name = "tenantId") Long tenantId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds, tenantId);
        return Result.success();
    }
}


