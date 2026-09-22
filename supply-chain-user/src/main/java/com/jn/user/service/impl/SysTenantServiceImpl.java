package com.jn.user.service.impl;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.BusinessException;
import com.jn.common.context.UserContext;
import com.jn.user.entity.SysDept;
import com.jn.user.entity.SysMenu;
import com.jn.user.entity.SysRole;
import com.jn.user.entity.SysTenant;
import com.jn.user.entity.SysUser;
import com.jn.user.mapper.SysDeptMapper;
import com.jn.user.mapper.SysMenuMapper;
import com.jn.user.mapper.SysRoleMapper;
import com.jn.user.mapper.SysTenantMapper;
import com.jn.user.mapper.SysUserMapper;
import com.jn.user.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @InterceptorIgnore(tenantLine = "true") // 【核心】：优雅地忽略当前方法的租户拦截，允许跨租户操作
    public void createTenant(SysTenant tenant
//            , String adminUsername, String adminPassword
    ) {
        // 1. 校验编码唯一性
        if (this.count(new LambdaQueryWrapper<SysTenant>().eq(SysTenant::getCode, tenant.getCode())) > 0) {
            throw new BusinessException("租户编码已存在");
        }

        // 2. 保存租户 (此时不会自动拼接 tenant_id 条件)
        this.save(tenant);
//        Long tenantId = tenant.getId();
//
//        // 3. 初始化默认部门
//        SysDept dept = new SysDept();
//        dept.setTenantId(tenantId);
//        dept.setName("总经办");
//        deptMapper.insert(dept);
//
//        // 4. 初始化默认角色 (租户管理员)
//        SysRole role = new SysRole();
//        role.setTenantId(tenantId);
//        role.setCode("TENANT_ADMIN_" + tenantId);
//        role.setName("租户管理员");
//        roleMapper.insert(role);
//
//        // 5. 初始化默认用户
//        SysUser user = new SysUser();
//        user.setTenantId(tenantId);
//        user.setDeptId(dept.getId());
//        user.setUsername(adminUsername);
//        user.setPassword(passwordEncoder.encode(adminPassword));
//        user.setNickname("管理员");
//        user.setStatus(1);
//        userMapper.insert(user);
//
//        // 6. 关联用户和角色 (sys_user_role 无 tenant_id)
//        userMapper.insertUserRoleBatch(user.getId(), List.of(role.getId()));
//
//        // 7. 给默认角色分配所有菜单权限 (sys_role_menu 无 tenant_id)
//        List<Long> allMenuIds = menuMapper.selectList(null).stream()
//                .map(SysMenu::getId).collect(Collectors.toList());
//        roleMapper.insertRoleMenuBatch(role.getId(), allMenuIds);
    }

    @Override
    public List<SysTenant> getAccessibleTenants() {
        Long currentTenantId = UserContext.getTenantId();

        // 1. 平台超级管理员 (tenant_id = 0)：拥有全局视野，查看所有租户
        if (currentTenantId != null && currentTenantId == 0L) {
            return this.list();
        }
        // 2. 普通租户管理员 (tenant_id != 0)：只能查看自己所在的租户信息
        else if (currentTenantId != null) {
            SysTenant tenant = this.getById(currentTenantId);
            // 包装成 List 返回，保持与超管查询时的数据结构一致，防止前端报错
            return tenant != null ? Collections.singletonList(tenant) : Collections.emptyList();
        }

        // 3. 上下文丢失或未登录状态：返回空列表
        return Collections.emptyList();
    }

    /**
     * 安全删除租户 (带事务与多重校验)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean safeRemoveTenant(Long tenantId) {
        // 【防护1】：禁止删除平台超级租户
        if (tenantId == null || tenantId == 0L) {
            throw new BusinessException("禁止删除平台超级租户");
        }

        // 【防护2】：检查租户是否存在
        SysTenant tenant = this.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在或已被删除");
        }

        // 【防护3】：检查租户下是否还存在有效用户
        // 防止误删导致大量用户账号瞬间失效
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId));
        if (userCount > 0) {
            throw new BusinessException(String.format("删除失败：该租户下仍存在 %d 个用户，请先迁移或禁用相关用户", userCount));
        }

        // TODO: 【扩展点】未来可在此处增加其他关联数据的检查
        // 例如：检查是否有未完成的订单、未结算的账单等
        // long orderCount = orderMapper.selectCount(...);
        // if (orderCount > 0) throw new BusinessException("该租户下存在未完成订单，无法删除");

        // 校验通过，执行物理删除
        return this.removeById(tenantId);
    }


}

