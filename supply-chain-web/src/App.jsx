import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { ConfigProvider, Layout, Menu, Button, Typography, Dropdown, Space, Avatar, message } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './pages/Login';
import { LogoutOutlined, DownOutlined, SwapOutlined, UserOutlined } from '@ant-design/icons';
import { useMemo } from 'react';
import { componentMap, iconMap } from './router/mappings';

// 【新增 1】：引入数据大屏组件 (请确保路径与您实际创建的文件路径一致)
import InventoryDashboard from './pages/dashboard/inventoryDashboard';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;

const RequireAuth = ({ children }) => {
    const { token } = useAuth();
    return token ? children : <Navigate to="/login" replace />;
};

const MainLayout = () => {
    // ... (此处保持您原有的 MainLayout 代码完全不变) ...
    const { user, logout, hasPermission } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const dynamicMenuItems = useMemo(() => {
        if (!user || !user.menus) return [];
        const generateItems = (menus) => {
            return menus
                .filter(menu => menu.path && menu.path.trim() !== '')
                .map(menu => {
                    const Icon = iconMap[menu.icon] || null;
                    const absolutePath = menu.path.startsWith('/') ? menu.path : `/${menu.path}`;
                    if (menu.children && menu.children.length > 0) {
                        return { key: absolutePath, icon: Icon, label: menu.name, children: generateItems(menu.children) };
                    }
                    return { key: absolutePath, icon: Icon, label: menu.name };
                });
        };
        return generateItems(user.menus);
    }, [user]);

    const dynamicRoutes = useMemo(() => {
        if (!user || !user.menus) return null;
        const routes = [];
        const traverse = (menus) => {
            menus.forEach(menu => {
                if (menu.menuType === 'C' && menu.component && menu.path && menu.path.trim() !== '') {
                    const Component = componentMap[menu.component];
                    if (Component) {
                        const relativePath = menu.path.startsWith('/') ? menu.path.substring(1) : menu.path;
                        routes.push(<Route key={menu.path} path={relativePath} element={<Component />} />);
                    }
                }
                if (menu.children) traverse(menu.children);
            });
        };
        traverse(user.menus);
        return routes;
    }, [user]);

    const defaultRedirectPath = useMemo(() => {
        if (!user || !user.menus) return '/404';
        const findFirstPath = (menus) => {
            for (const menu of menus) {
                if (menu.menuType === 'C' && menu.path) return menu.path.startsWith('/') ? menu.path : `/${menu.path}`;
                if (menu.children) { const found = findFirstPath(menu.children); if (found) return found; }
            }
            return null;
        };
        return findFirstPath(user.menus) || '/404';
    }, [user]);

    const selectedKeys = [location.pathname];
    const openKeys = useMemo(() => ['/system'], []);

    const handleSwitchTenant = () => {
        message.info('切换租户需要重新登录，正在跳转...');
        setTimeout(() => { logout(); navigate('/login'); }, 1000);
    };

    const userMenuItems = [
        { key: 'switch', icon: <SwapOutlined />, label: '切换租户', onClick: handleSwitchTenant },
        { type: 'divider' },
        { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: logout }
    ];

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <Sider theme="light" width={220} style={{ boxShadow: '2px 0 8px 0 rgba(29,35,41,.05)' }}>
                <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid #f0f0f0' }}>
                    <Title level={4} style={{ margin: 0, color: '#1890ff' }}>自动配组系统</Title>
                </div>
                <Menu mode="inline" selectedKeys={selectedKeys} defaultOpenKeys={openKeys} items={dynamicMenuItems} onClick={(e) => navigate(e.key)} style={{ borderRight: 0 }} />
            </Sider>
            <Layout>
                <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', boxShadow: '0 1px 4px rgba(0,0,0,0.05)' }}>
                    <Space size="large">
                        <Text type="secondary">当前租户：<Text strong>{user?.tenantId === 0 ? '平台管理中心' : `租户ID: ${user?.tenantName}`}</Text></Text>
                        <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                            <Space style={{ cursor: 'pointer' }}>
                                <Avatar size="small" icon={<UserOutlined />} />
                                <Text>{user?.username || '用户'}</Text>
                                <DownOutlined />
                            </Space>
                        </Dropdown>
                    </Space>
                </Header>
                <Content style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8, minHeight: 280 }}>
                    <Routes>
                        {dynamicRoutes}
                        <Route path="*" element={<Navigate to={defaultRedirectPath} replace />} />
                    </Routes>
                </Content>
            </Layout>
        </Layout>
    );
};

function App() {
    return (
        <ConfigProvider locale={zhCN}>
            <AuthProvider>
                <BrowserRouter>
                    <Routes>
                        {/* 1. 登录页 (独立 Layout) */}
                        <Route path="/login" element={<Login />} />

                        {/* 【新增 2】：数据大屏 (独立 Layout，拦截在 MainLayout 之前)
                          如果不需要登录，改成下面内容
                          <Route path="/dashboard/inventory" element={<InventoryDashboard />} />
                         */}
                        <Route
                            path="/dashboard/inventory"
                            element={
                                <RequireAuth>
                                    <InventoryDashboard />
                                </RequireAuth>
                            }
                        />

                        {/* 2. 主系统 Layout (带侧边栏和顶栏) */}
                        <Route path="/*" element={<RequireAuth><MainLayout /></RequireAuth>} />
                    </Routes>
                </BrowserRouter>
            </AuthProvider>
        </ConfigProvider>
    );
}

export default App;
