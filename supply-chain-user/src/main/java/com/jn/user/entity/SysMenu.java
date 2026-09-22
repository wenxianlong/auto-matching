package com.jn.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO) // 菜单通常数据量小，使用自增ID即可
    private Long id;
    private Long parentId;
    private Long tenantId;
    private String name;
    private String path;
    private String component;
    private String perms;
    private String menuType; // M:目录 C:菜单 F:按钮
    private String icon;
    private Integer sortOrder;
    private Integer status;

    @TableField(exist = false)
    private List<SysMenu> children; // 用于构建菜单树
}

