package com.jn.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jn.user.entity.SysRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据角色ID删除该角色所有的菜单权限关联
     *
     * @param roleId 角色ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenuByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色与菜单的关联关系
     * 注意：在注解中使用动态 SQL（如 foreach），必须包裹在 <script> 标签中
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID集合
     * @return 影响的行数
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleId}, #{menuId})" +
            "</foreach>" +
            "</script>")
    int insertRoleMenuBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 根据角色ID查询已分配的菜单ID集合
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID查询已分配的菜单ID集合
     */
    @Select("SELECT rm.menu_id FROM sys_role_menu rm left join sys_menu sm on sm.id = rm.menu_id WHERE rm.role_id = #{roleId} and sm.tenant_id = #{tenantId}" )
    List<Long> selectMenuIdsByRoleIdAndTenantId(@Param("roleId") Long roleId,@Param("tenantId") Long tenantId);

}
