import { useAuth } from '../contexts/AuthContext';

export default function AuthButton({ perm, children, ...props }) {
    const { user } = useAuth();
    // 如果是超级管理员，直接显示
    if (user?.roles?.includes('ADMIN')) return <button {...props}>{children}</button>;

    // 否则校验权限标识
    const hasPerm = user?.permissions?.includes(perm);
    return hasPerm ? <button {...props}>{children}</button> : null;
}

// 使用示例：
// <AuthButton perm="sys:user:add" onClick={handleAdd}>新增用户</AuthButton>
