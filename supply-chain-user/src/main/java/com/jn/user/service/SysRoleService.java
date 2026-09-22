package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {
    void assignMenus(Long roleId, java.util.List<Long> menuIds, Long tenantId);
}
