import request from './request';

export const getCategoryPageApi = (params) => {
    return request.get('/admin/categories/page', { params });
};
