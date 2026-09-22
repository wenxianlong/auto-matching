import { useAuth } from '../contexts/AuthContext.jsx';

/**
 * 权限控制组件
 * @param {string} perm - 权限标识，如 'sys:user:add'
 * @param {ReactNode} children - 子元素
 */
export default function Auth({ perm, children }) {
    const { hasPermission } = useAuth();

    // 如果有权限，渲染子元素；否则渲染 null (隐藏)
    return hasPermission(perm) ? children : null;
}
