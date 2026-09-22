package com.jn.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jn.user.entity.SysDept;
import com.jn.user.entity.SysUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {
    List<SysDept> buildDeptTree(List<SysDept> depts);

    @Transactional(rollbackFor = Exception.class)
    void createDept(SysDept dept);

    @Transactional(rollbackFor = Exception.class)
    void updateDept(SysDept dept);

    void deleteDept(Long deptId);
}
