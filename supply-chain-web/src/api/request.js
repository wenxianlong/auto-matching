import axios from 'axios';
import { message } from 'antd';
import JSONBig from 'json-bigint';

// 配置 json-bigint，将超出安全范围的大数字自动转为字符串
const JSONBigNative = JSONBig({ storeAsString: true });

const request = axios.create({
    // 【修改】：统一加上 /api 前缀，配合网关的 StripPrefix=1
    // 实际请求如：/api/admin/system/user/page
    baseURL: '/api',
    timeout: 10000,
    // 【核心新增】：自定义响应数据转换，使用 json-bigint 解析
    transformResponse: [function (data) {
        try {
            // 如果 data 是字符串，使用 JSONBig 解析
            if (typeof data === 'string') {
                return JSONBigNative.parse(data);
            }
            return data;
        } catch (e) {
            // 解析失败（如返回的是纯文本或 HTML），直接返回原数据
            return data;
        }
    }]
});

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        // 【核心新增】：超管跨租户请求自动注入 Header
        const userInfoStr = localStorage.getItem('userInfo'); // 假设登录时存了用户信息
        const currentTargetTenant = localStorage.getItem('currentTargetTenantId'); // 超管选中的租户ID

        if (userInfoStr && currentTargetTenant) {
            try {
                const user = JSON.parse(userInfoStr);
                // 如果是超管 (tenantId === 0)，且选中了目标租户
                if (user.tenantId === 0 && currentTargetTenant !== '0') {
                    config.headers['X-Target-Tenant-Id'] = currentTargetTenant;
                }
            } catch (e) {}
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// 响应拦截器：统一处理错误
request.interceptors.response.use(
    (response) => {
        const res = response.data;
        // 如果是 OAuth2 的 token 接口，直接返回
        if (response.config.url.includes('/oauth2/token') || response.config.url.includes('/user-service/auth/login')) {
            return res;
        }
        // 业务接口统一判断 code
        if (res.code !== 200) {
            message.error(res.msg || '请求失败');
            if (res.code === 401) {
                localStorage.removeItem('token');
                window.location.href = '/login';
            }
            return Promise.reject(new Error(res.msg || 'Error'));
        }
        return res.data; // 直接返回 data 层
    },
    (error) => {
        message.error(error.message || '网络错误');
        return Promise.reject(error);
    }
);

export default request;
