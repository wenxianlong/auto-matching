package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    void createUser(SysUser user);
    void updateUser(SysUser user);
}
