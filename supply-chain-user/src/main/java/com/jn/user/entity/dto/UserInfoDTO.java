package com.jn.user.entity.dto;

import com.jn.user.entity.SysMenu;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private String nickname;
    private Long tenantId;
    private String tenantName;
    private Long deptId;
    private Set<String> roles;
    private Set<String> permissions; // 权限标识集合
    private List<SysMenu> menus;     // 菜单树 (用于前端渲染路由)
}

