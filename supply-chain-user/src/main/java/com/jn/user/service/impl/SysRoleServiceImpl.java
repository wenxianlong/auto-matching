package com.jn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.user.entity.SysMenu;
import com.jn.user.entity.SysRole;
import com.jn.user.mapper.SysMenuMapper;
import com.jn.user.mapper.SysRoleMapper;
import com.jn.user.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds, Long tenantId) {
        // 1. 删除该角色原有的所有菜单关联
        baseMapper.deleteRoleMenuByRoleId(roleId);
        // 如果前端传来的菜单 ID 为空，说明取消了所有权限，直接返回
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }

        // ================= 【核心修复】：自动补全父级菜单 ID =================

        // 2. 查询系统中所有的菜单（菜单表数据量通常很小，全量加载到内存处理最快且无 SQL 递归兼容性问题）
        LambdaQueryWrapper<SysMenu> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysMenu::getTenantId,tenantId);
        List<SysMenu> allMenus = sysMenuMapper.selectList(lambdaQueryWrapper);

        // 构建 ID -> Menu 的 Map，方便 O(1) 复杂度快速查找父节点
        Map<Long, SysMenu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m, (k1, k2) -> k1));

        // 使用 Set 自动去重，初始化时放入前端传来的 ID
        Set<Long> finalMenuIds = new HashSet<>(menuIds);

        // 3. 遍历前端传来的 ID，沿着 parentId 向上追溯，把所有祖先节点加进 Set 中
        for (Long menuId : menuIds) {
            Long currentId = menuId;

            // 循环向上找父节点，直到根节点 (parentId == 0) 或找不到为止
            while (currentId != null && currentId != 0L) {
                finalMenuIds.add(currentId); // 将当前节点加入最终集合

                SysMenu currentMenu = menuMap.get(currentId);
                if (currentMenu != null && currentMenu.getParentId() != null) {
                    currentId = currentMenu.getParentId(); // 指针移向父节点
                } else {
                    break; // 找不到父节点信息，终止向上追溯
                }
            }
        }

        // 4. 批量插入补全父节点后的完整关系数据
        baseMapper.insertRoleMenuBatch(roleId, new ArrayList<>(finalMenuIds));
    }
}

