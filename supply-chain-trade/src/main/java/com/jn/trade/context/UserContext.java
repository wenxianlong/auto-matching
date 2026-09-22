package com.jn.trade.context;

public class UserContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_MANAGER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) { TENANT_ID.set(tenantId); }
    public static Long getTenantId() { return TENANT_ID.get(); }

    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }

    public static void setIsManager(Boolean isManager) { IS_MANAGER.set(isManager); }
    public static Boolean isManager() { return Boolean.TRUE.equals(IS_MANAGER.get()); }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        DEPT_ID.remove();
        IS_MANAGER.remove();
    }
}

