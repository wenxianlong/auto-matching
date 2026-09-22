package com.jn.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jn.common.BusinessException;
import com.jn.common.context.UserContext;
import com.jn.user.entity.SysDept;
import com.jn.user.entity.SysUser;
import com.jn.user.mapper.SysDeptMapper;
import com.jn.user.mapper.SysUserMapper;
import com.jn.user.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        return depts.stream()
                .filter(dept -> dept.getParentId() == 0)
                .peek(dept -> dept.setChildren(getChildren(dept, depts)))
                .sorted((d1, d2) -> d1.getSortOrder() - d2.getSortOrder())
                .collect(Collectors.toList());
    }

    private List<SysDept> getChildren(SysDept root, List<SysDept> allDepts) {
        return allDepts.stream()
                .filter(dept -> dept.getParentId().equals(root.getId()))
                .peek(dept -> dept.setChildren(getChildren(dept, allDepts)))
                .sorted((d1, d2) -> d1.getSortOrder() - d2.getSortOrder())
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createDept(SysDept dept) {
        // 1. 处理租户ID (超管可指定，普通管理员使用自己的)
        if (dept.getTenantId() == null) {
            dept.setTenantId(UserContext.getTenantId());
        }

        // 2. 构建 ancestors
        if (dept.getParentId() == 0) {
            dept.setAncestors("0");
        } else {
            SysDept parentDept = this.getById(dept.getParentId());
            if (parentDept == null) throw new BusinessException("上级部门不存在");
            dept.setAncestors(parentDept.getAncestors() + "," + parentDept.getId());
        }
        this.save(dept);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDept(SysDept dept) {
        SysDept oldDept = this.getById(dept.getId());
        if (oldDept == null) throw new BusinessException("部门不存在");

        // 如果修改了父部门，需要更新自己和所有子部门的 ancestors
        if (!oldDept.getParentId().equals(dept.getParentId())) {
            if (dept.getParentId() == 0) {
                dept.setAncestors("0");
            } else {
                SysDept newParent = this.getById(dept.getParentId());
                dept.setAncestors(newParent.getAncestors() + "," + newParent.getId());
            }
            // 更新子部门的 ancestors
            updateChildrenAncestors(dept.getId(), dept.getAncestors(), oldDept.getAncestors());
        }
        this.updateById(dept);
    }

    private void updateChildrenAncestors(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = this.list(new LambdaQueryWrapper<SysDept>()
                .like(SysDept::getAncestors, deptId));
        if (children != null && !children.isEmpty()) {
            for (SysDept child : children) {
                child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            }
            this.updateBatchById(children);
        }
    }

    @Override
    public void deleteDept(Long deptId) {
        // 1. 校验是否有子部门
        long childCount = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, deptId));
        if (childCount > 0) throw new BusinessException("存在子部门，不允许删除");

        // 2. 校验是否有用户
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, deptId));
        if (userCount > 0) throw new BusinessException("部门下存在用户，不允许删除");

        this.removeById(deptId);
    }
}
