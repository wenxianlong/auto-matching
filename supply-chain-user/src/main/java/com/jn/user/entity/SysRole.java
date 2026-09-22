package com.jn.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.List;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String code;
    private String name;

    // 【新增】：数据范围（1：全部数据 2：本部门及以下数据 3：本部门数据 4：仅本人数据 5：自定义数据）
    private Integer dataScope;

    // 在 SysRole.java 中增加以下字段
    private Long deptId;

    @TableField(exist = false)
    private List<Long> menuIds; // 关联的菜单ID列表
}

