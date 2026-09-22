import { UserOutlined, TeamOutlined, MenuOutlined, AppstoreOutlined } from '@ant-design/icons';
import UserManagement from '../pages/system/user';
import RoleManagement from '../pages/system/role';
import MenuManagement from '../pages/system/menu';
import Category from '../pages/category';
import TenantManagement from '../pages/system/tenant';
import DeptManagement from '../pages/system/dept';
import GroupingManagement from '../pages/mfg/grouping';
import DictManagement from '../pages/system/dict';
import CategoryManagement from '../pages/business/category';
import ContractManagement from '../pages/business/contract/create.jsx'; // 假设您后续开发了合同列表页

// 1. 路由组件映射表 (Key 必须与数据库 sys_menu 表的 component 字段一致)
export const componentMap = {
    'system/user/index': UserManagement,
    'system/role/index': RoleManagement,
    'system/menu/index': MenuManagement,
    'system/tenant/index': TenantManagement,
    'system/dept/index': DeptManagement,
    'system/dict/index': DictManagement,
    'category/index': Category,
    'mfg/grouping/index': GroupingManagement,
    'business/category/index': CategoryManagement,
    'business/contract/index': ContractManagement,

};

// 2. 图标映射表 (Key 必须与数据库 sys_menu 表的 icon 字段一致)
export const iconMap = {
    UserOutlined: <UserOutlined />,
    TeamOutlined: <TeamOutlined />,
    MenuOutlined: <MenuOutlined />,
    AppstoreOutlined: <AppstoreOutlined />,
};
