import request from './request';
export const getDictDataApi1 = (dictType) => request.get(`/admin/system/dict/data/${dictType}`);


export const getDictDataApi = (params) => request.get('/admin/system/dict/data/list', { params });




