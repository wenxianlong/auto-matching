import { Outlet } from 'react-router-dom';

/**
 * 空白布局：用于登录页、数据大屏等不需要侧边栏和顶栏的独立页面
 */
export default function BlankLayout() {
    return <Outlet />;
}
