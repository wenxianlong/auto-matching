package com.jn.user.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jn.user.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @InterceptorIgnore(tenantLine = "true")
    // 根据用户ID查询权限标识
    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.perms IS NOT NULL AND m.perms != ''")
    List<String> loginSelectPermsByUserId(Long userId);

    // 根据用户ID查询菜单列表（用于构建前端路由树）
    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.tenant_id = #{tenantId} " +
            "AND m.status = 1 " +
            "AND m.menu_type IN ('M', 'C') " +
            "ORDER BY m.sort_order")
    List<SysMenu> selectMenusByUserId(@Param("userId")Long userId,@Param("tenantId") Long tenantId);

    /**
     * 根据菜单ID删除角色与菜单的关联关系
     */
    @Delete("DELETE FROM sys_role_menu WHERE menu_id = #{menuId}")
    int deleteRoleMenuByMenuId(@Param("menuId") Long menuId);
}
