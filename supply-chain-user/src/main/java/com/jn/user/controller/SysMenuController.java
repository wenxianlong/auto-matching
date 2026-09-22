package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysMenu;
import com.jn.user.entity.SysRole;
import com.jn.user.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /**
     * 获取菜单列表 (返回扁平列表，前端自行构建树，或后端构建树均可)
     * 这里返回扁平列表，按 sort_order 排序
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:menu:list')")
    public Result<List<SysMenu>> list(@RequestParam(required = false) Long tenantId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(SysMenu::getTenantId, tenantId);
        }
        return Result.success( menuService.list(wrapper));
    }

    /**
     * 获取菜单树 (供角色分配权限时使用，不需要按钮级权限校验，只要有角色管理权限即可)
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('sys:role:list') or hasAuthority('sys:menu:list')")
    public Result<List<SysMenu>> tree() {
        List<SysMenu> menus = menuService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSortOrder)
        );
        return Result.success(menuService.buildMenuTree(menus));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('sys:menu:add')")
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.createMenu(menu);
        return Result.success();
    }

    /**
     * 修改菜单
     */
    @PutMapping
    @PreAuthorize("hasAuthority('sys:menu:edit')")
    public Result<Void> edit(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return Result.success();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:menu:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
