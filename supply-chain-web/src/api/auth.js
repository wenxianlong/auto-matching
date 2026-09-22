import request from './request';

export const loginApi = (username, password) => {
    return request.post('/admin/system/auth/login', { username, password });
};

export const getUserInfoApi = () => {
    return request.get('/admin/system/user/info');
};


// 【修改 3】：退出登录接口（如果有的话）
export const logoutApi = () => {
    return request.post('/admin/system/auth/logout');
};