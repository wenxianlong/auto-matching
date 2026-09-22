import request from './request';

// ================= 用户管理 =================
export const getUserPageApi = (params) => request.get('/admin/system/user/list', { params });
export const saveUserApi = (data) => data.id ? request.put('/admin/system/user', data) : request.post('/admin/system/user', data);
export const deleteUserApi = (id) => request.delete(`/admin/system/user/${id}`);
// 【新增】获取用户已分配的角色ID列表
export const getUserRolesApi = (userId) => request.get(`/admin/system/user/${userId}/roles`);
// 【新增】给用户分配角色
export const assignUserRolesApi = (userId, tenantId, roleIds) => request.post(`/admin/system/user/${userId}/${tenantId}/roles`, roleIds);

// ================= 角色管理 =================
export const getRoleListApi = (params) => request.get('/admin/system/role/list', { params });
export const saveRoleApi = (data) => data.id ? request.put('/admin/system/role', data) : request.post('/admin/system/role', data);
export const deleteRoleApi = (id) => request.delete(`/admin/system/role/${id}`);

// 获取角色已分配的菜单ID (必须是 GET)
export const getRoleMenusApi = (roleId,tenantId) => request.get(`/admin/system/role/${roleId}/${tenantId}/menus`, );
// 分配菜单权限 (必须是 POST)
export const assignRoleMenusApi = (roleId, tenantId, menuIds ) => request.post(`/admin/system/role/${roleId}/${tenantId}/menus`, menuIds);

// ================= 菜单管理 =================
export const getMenuListApi = (params) => request.get('/admin/system/menu/list', { params });
export const saveMenuApi = (data) => data.id ? request.put('/admin/system/menu', data) : request.post('/admin/system/menu', data);
export const deleteMenuApi = (id) => request.delete(`/admin/system/menu/${id}`);

// 租户管理
export const getTenantListApi = () => request.get('/admin/system/tenant/list');
/**
 * 新增租户
 * @param {Object} data - 租户数据 { name, code, contactUser, contactPhone, status }
 */
export const addTenantApi = (data) => {
    return request.post('/admin/system/tenant', data);
};

/**
 * 修改租户
 * @param {Object} data - 租户数据 { id, name, code, contactUser, contactPhone, status }
 */
export const updateTenantApi = (data) => {
    return request.put('/admin/system/tenant', data);
};

/**
 * 删除租户
 * @param {number|string} id - 租户ID
 */
export const deleteTenantApi = (id) => {
    return request.delete(`/admin/system/tenant/${id}`);
};

// ================= 部门管理 =================
export const getDeptListApi = (params) => request.get('/admin/system/dept/list', { params });
export const getDeptTreeApi = (params) => request.get('/admin/system/dept/tree', { params });
export const saveDeptApi = (data) => data.id ? request.put('/admin/system/dept', data) : request.post('/admin/system/dept', data);
export const deleteDeptApi = (id) => request.delete(`/admin/system/dept/${id}`);





