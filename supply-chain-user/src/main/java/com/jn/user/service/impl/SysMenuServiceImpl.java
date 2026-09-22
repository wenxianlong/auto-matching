package com.jn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.BusinessException;
import com.jn.user.entity.SysMenu;
import com.jn.user.mapper.SysMenuMapper;
import com.jn.user.mapper.SysRoleMapper; // 需要注入角色 Mapper 来校验关联
import com.jn.user.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysRoleMapper roleMapper; // 用于校验角色关联

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 强制让用户的旧 Token 失效
     */
    public void invalidateUserToken(Long userId) {
        String key = "user:token:valid_time:" + userId;
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // 【修改点】：使用 StringRedisTemplate 写入，确保在 Redis 中存的是纯数字字符串，与网关读取端完美契合
        stringRedisTemplate.opsForValue().set(key, String.valueOf(currentTimeSeconds), 2, TimeUnit.HOURS);
    }

    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        return menus.stream()
                .filter(menu -> menu.getParentId() == 0) // 找到根节点
                .peek(menu -> menu.setChildren(getChildren(menu, menus)))
                .collect(Collectors.toList());
    }

    private List<SysMenu> getChildren(SysMenu root, List<SysMenu> allMenus) {
        return allMenus.stream()
                .filter(menu -> menu.getParentId().equals(root.getId()))
                .peek(menu -> menu.setChildren(getChildren(menu, allMenus)))
                .collect(Collectors.toList());
    }

    @Override
    public void createMenu(SysMenu menu) {
        // 校验同级菜单下名称是否重复
        long count = this.count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menu.getParentId())
                .eq(SysMenu::getName, menu.getName()));
        if (count > 0) {
            throw new BusinessException("同级菜单下名称不能重复");
        }
        this.save(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        // 不能将自己设置为自己的父菜单
        if (menu.getId().equals(menu.getParentId())) {
            throw new BusinessException("上级菜单不能选择自己");
        }
        this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        // 1. 校验是否存在子菜单
        long childCount = this.count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new BusinessException("存在子菜单，不允许删除");
        }

        // 2. 校验是否被角色关联 (查询 sys_role_menu 表)
        // 假设 SysRoleMapper 中有个方法可以查，或者直接用 MyBatis-Plus 的通用方法（如果没配实体，可以用原生 SQL）
        // 这里为了简单，我们直接在 SysMenuMapper 中写一个统计方法，或者忽略此校验。
        // 严谨做法：如果存在关联，先删除 sys_role_menu 中的关联数据，再删除菜单。
        baseMapper.deleteRoleMenuByMenuId(menuId); // 需要在 SysMenuMapper 中补充此方法

        // 3. 删除菜单
        this.removeById(menuId);
    }
}

