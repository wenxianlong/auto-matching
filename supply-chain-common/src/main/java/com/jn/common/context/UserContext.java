package com.jn.common.context;

public class UserContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    //数据范围
    private static final ThreadLocal<Integer> DATA_SCOPE = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> SUPER_ADMIN = new ThreadLocal<>();

    // 【新增】：超管跨租户查询时，临时指定的目标租户ID
    private static final ThreadLocal<Long> TARGET_TENANT_ID = new ThreadLocal<>();

    public static void setTargetTenantId(Long tenantId) { TARGET_TENANT_ID.set(tenantId); }
    public static Long getTargetTenantId() { return TARGET_TENANT_ID.get(); }

    public static void setTenantId(Long tenantId) { TENANT_ID.set(tenantId); }
    public static Long getTenantId() { return TENANT_ID.get(); }

    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }

    public static void setDataScope(Integer dataScope) { DATA_SCOPE.set(dataScope); }
    public static Integer getDataScope() { return DATA_SCOPE.get(); }

    public static void setSuperAdmin(Boolean isSuperAdmin) { SUPER_ADMIN.set(isSuperAdmin); }
    // 判断是否为超管 (假设 tenantId == 0 为超管)
    public static boolean isSuperAdmin() {
        Long tenantId = TENANT_ID.get();
        return tenantId != null && tenantId == 0L;
    }
    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        DEPT_ID.remove();
        DATA_SCOPE.remove();
        SUPER_ADMIN.remove();
        TARGET_TENANT_ID.remove();
    }



}

