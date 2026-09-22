import { createContext, useState, useContext, useEffect } from 'react';
import { loginApi, getUserInfoApi } from '../api/auth';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (token) {
            fetchUserInfo();
        }
    }, [token]);

    const fetchUserInfo = async () => {
        try {
            const data = await getUserInfoApi();
            setUser(data);
        } catch (error) {
            logout();
        }
    };

    const login = async (username, password) => {
        setLoading(true);
        try {
            const res = await loginApi(username, password);
            localStorage.setItem('token', res.access_token);
            setToken(res.access_token);
            return true;
        } finally {
            setLoading(false);
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
    };

    // 【核心新增】：判断当前用户是否拥有某个权限
    const hasPermission = (perm) => {
        if (!user) return false;
        // 超级管理员拥有所有权限
        if (user.roles && user.roles.includes('ADMIN')) return true;
        // 校验具体权限标识
        return user.permissions && user.permissions.includes(perm);
    };

    return (
        <AuthContext.Provider value={{ user, token, loading, login, logout, hasPermission }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
