package com.jn.user.controller;

import com.jn.common.Result;
import com.jn.user.entity.SysTenant;
import com.jn.user.service.SysTenantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService tenantService;

    /**
     * 获取租户列表 (已做数据权限隔离)
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:tenant:list') or hasAuthority('*:*:*')")
    public Result<List<SysTenant>> list() {
        // 【修改点】：调用带有越权防护的方法
        return Result.success(tenantService.getAccessibleTenants());
    }

    @DeleteMapping("/{id}") // 确认这里是 /{id} 而不是 /delete/{id} 或其他
    @PreAuthorize("hasAuthority('sys:tenant:remove') or hasAuthority('*:*:*')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        // 不再直接调用 removeById，而是调用带校验的安全删除
        tenantService.safeRemoveTenant(id);
        return Result.success();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:tenant:add') or hasAuthority('*:*:*')")
    public Result<?> create(@RequestBody TenantCreateDTO dto) {
        SysTenant tenant = new SysTenant();
        tenant.setName(dto.getName());
        tenant.setCode(dto.getCode());
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setStatus(dto.getStatus());
        tenant.setContactUser(dto.contactUser);
        tenant.setContactPhone(dto.contactPhone);
        tenantService.createTenant(tenant);
        return Result.success();
    }

    @Data
    public static class TenantCreateDTO {
        private String name;
        private String code;
        private String contactUser;
        private String contactPhone;
        private Integer status;
    }
}
