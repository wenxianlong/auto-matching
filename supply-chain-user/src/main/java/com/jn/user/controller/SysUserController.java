package com.jn.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jn.common.Result;
import com.jn.common.utils.RedisUtils;
import com.jn.user.entity.SysMenu;
import com.jn.user.entity.SysRole;
import com.jn.user.entity.SysTenant;
import com.jn.user.entity.SysUser;
import com.jn.user.entity.dto.UserInfoDTO;
import com.jn.user.mapper.SysMenuMapper;
import com.jn.user.mapper.SysRoleMapper;
import com.jn.user.mapper.SysUserMapper;
import com.jn.user.service.SysMenuService;
import com.jn.user.service.SysTenantService;
import com.jn.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/admin/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysMenuMapper menuMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysMenuService menuService;

    private final SysUserMapper userMapper;

    private final SysUserService userService;

    private final SysTenantService tenantService;


    private final RedisUtils redisUtils; // 注入工具类

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取当前登录用户的详细信息、权限和菜单树
     * 【关键修改】：参数改为接收 JwtAuthenticationToken 或直接接收 Jwt
     */
    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo(JwtAuthenticationToken authentication) {
        Long userId = ((Jwt) authentication.getPrincipal()).getClaim("user_id");
        String cacheKey = "user:info:" + userId;

        // 【优化】：直接获取并自动转换为 UserInfoDTO，不再有 ClassCastException
        UserInfoDTO cachedInfo = redisUtils.get(cacheKey, UserInfoDTO.class);
        if (cachedInfo != null) {
            return Result.success(cachedInfo);
        }
        // 1. 从 JWT 中获取 Principal (即 Jwt 对象)
        Jwt jwt = authentication.getToken();

        UserInfoDTO dto = new UserInfoDTO();

        // 2. 从 JWT Claims 中解析我们自定义的字段
        dto.setUserId(jwt.getClaim("user_id"));
        dto.setUsername(jwt.getSubject()); // subject 就是 username
        dto.setTenantId(jwt.getClaim("tenant_id"));
        SysTenant tenant = tenantService.getById(jwt.getClaim("tenant_id"));
        dto.setTenantName(tenant.getName());

        dto.setDeptId(jwt.getClaim("dept_id"));

        // 3. 解析角色和权限
        List<String> authorities = jwt.getClaimAsStringList("roles");
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();

        if (authorities != null) {
            for (String auth : authorities) {
                if (auth.startsWith("ROLE_")) {
                    roles.add(auth.replace("ROLE_", ""));
                } else {
                    permissions.add(auth);
                }
            }
        }

        // 兜底：如果 JWT 里没有 roles 字段，从 authentication 里取
        if (roles.isEmpty() && permissions.isEmpty()) {
            authentication.getAuthorities().forEach(auth -> {
                String authStr = auth.getAuthority();
                if (authStr.startsWith("ROLE_")) roles.add(authStr.replace("ROLE_", ""));
                else permissions.add(authStr);
            });
        }

        dto.setRoles(roles);
        dto.setPermissions(permissions);

        // 4. 获取菜单树
        List<SysMenu> menus;
        if (roles.contains("ADMIN")) {
            menus = menuService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getMenuType, "M", "C")
                            .eq(SysMenu::getStatus, 1)
                            .orderByAsc(SysMenu::getSortOrder)
            );
        } else {
            menus = menuMapper.selectMenusByUserId(dto.getUserId(), dto.getTenantId());
        }
        dto.setMenus(menuService.buildMenuTree(menus));

        // 3. 写入 Redis，设置过期时间 (如 2 小时，与 JWT 过期时间一致)
        redisTemplate.opsForValue().set(cacheKey, dto, 2, TimeUnit.HOURS);

        return Result.success(dto);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:user:list')")
    public Result<Page<SysUser>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "tenantId") String tenantId,
            @RequestParam(value = "username", required = false) String username) {

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId);
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username);

        // 查询时排除密码字段，防止泄露
        wrapper.select(SysUser.class, info -> !info.getColumn().equals("password"));

        return Result.success(userService.page(page, wrapper));
    }

    /**
     * 新增用户 (解决 404 的核心接口)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('sys:user:add')")
    public Result<Void> add(@RequestBody SysUser user) {
        userService.createUser(user);
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping
    @PreAuthorize("hasAuthority('sys:user:edit')")
    public Result<Void> edit(@RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    /**
     * 获取用户已分配的角色ID列表
     */
    @GetMapping("/{userId}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long userId) {
        return Result.success(userMapper.selectRoleIdsByUserId(userId));
    }

    /**
     * 给用户分配角色
     */
    @PostMapping("/{userId}/{tenantId}/roles")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    public Result<Void> assignRoles(@PathVariable Long userId, @PathVariable Long tenantId, @RequestBody List<Long> roleIds) {
        userMapper.deleteUserRoleByUserId(userId);
        //查询传递的角色
        LambdaQueryWrapper<SysRole> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SysRole::getId,roleIds);
        List<SysRole> roleList = sysRoleMapper.selectList(lambdaQueryWrapper);
        //看角色是否是传入的租户的,过滤传入租户的角色
        List<Long> newRoleIds = roleList.stream().filter(item->item.getTenantId().equals(tenantId))
                .map(SysRole::getId)
                .toList();

        if (!newRoleIds.isEmpty()) {
            userMapper.insertUserRoleBatch(userId, newRoleIds);
        }
        return Result.success();
    }


}
