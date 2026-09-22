import request from './request';


// ================= 生产管理 =================
export const calculate = (data) =>  request.post(`/admin/business/mfg/grouping/calculate`, data);


// ==========================================
// 一、 品类管理 (B端管理后台)
// 路径前缀: /admin/business/category
// ==========================================

/**
 * 获取品类列表
 */
export const getCategoryListApi = () => {
    return request.get('/admin/business/category/list');
};

/**
 * 新增品类
 * @param {Object} data - 品类数据 { name, code }
 */
export const addCategoryApi = (data) => {
    return request.post('/admin/business/category', data);
};

/**
 * 修改品类
 * @param {Object} data - 品类数据 { id, name, code }
 */
export const updateCategoryApi = (data) => {
    return request.put('/admin/business/category', data);
};

/**
 * 删除品类
 * @param {number|string} id - 品类ID
 */
export const deleteCategoryApi = (id) => {
    return request.delete(`/admin/business/category/${id}`);
};

/**
 * 获取指定品类的字段配置
 * @param {number|string} categoryId - 品类ID
 */
export const getCategoryFieldsApi = (categoryId) => {
    return request.get(`/admin/business/category/${categoryId}/fields`);
};

/**
 * 保存指定品类的字段配置 (全量覆盖)
 * @param {number|string} categoryId - 品类ID
 * @param {Array} data - 字段配置数组
 */
export const saveCategoryFieldsApi = (categoryId, data) => {
    return request.post(`/admin/business/category/${categoryId}/fields`, data);
};


// ==========================================
// 二、 合同管理 (B端管理后台)
// 路径前缀: /admin/business/contract
// ==========================================

/**
 * 分页查询合同列表
 * @param {Object} params - 查询参数 { pageNum, pageSize, contractNo, categoryId, status }
 */
export const getContractPageApi = (params) => {
    return request.get('/admin/business/contract/page', { params });
};

/**
 * 获取合同详情 (包含动态字段 extraData)
 * @param {number|string} id - 合同ID
 */
export const getContractDetailApi = (id) => {
    return request.get(`/admin/business/contract/${id}`);
};

/**
 * 审核合同 / 变更合同状态
 * @param {Object} data - { id, status, remark }
 */
export const auditContractApi = (data) => {
    return request.put('/admin/business/contract/audit', data);
};

/**
 * 删除合同 (通常只允许删除草稿状态)
 * @param {number|string} id - 合同ID
 */
export const deleteContractApi = (id) => {
    return request.delete(`/admin/business/contract/${id}`);
};


// ==========================================
// 四、 租户品类授权 (B端超管专用)
// 路径前缀: /admin/trade/tenant-category
// ==========================================

/**
 * 获取指定租户已开通的品类ID列表
 */
export const getTenantCategoriesApi = (tenantId) => {
    return request.get(`/admin/business/tenant-category/${tenantId}`);
};

/**
 * 保存租户品类授权 (全量覆盖)
 */
export const saveTenantCategoriesApi = (tenantId, categoryIds) => {
    return request.post(`/admin/business/tenant-category/${tenantId}`, categoryIds);
};




// ==========================================
// 三、 合同管理 (C端用户/服务端)
// 路径前缀: /app/business/contract
// ==========================================

/**
 * 获取当前登录用户(租户)可用的品类列表
 */
export const getMyCategoriesApi = () => {
    return request.get('/app/business/contract/my-categories');
};

/**
 * C端用户创建/提交合同
 * @param {Object} data - 合同数据 { contractNo, categoryId, amount, extraData: {...} }
 */
export const createContractApi = (data) => {
    return request.post('/app/business/contract', data);
};
