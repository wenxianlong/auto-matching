import { useState, useEffect } from 'react';
import { Table, Button, Space, Card, Modal, Form, Input, Select, TreeSelect, message, Popconfirm, Tag, InputNumber } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {getMenuListApi, saveMenuApi, deleteMenuApi, getTenantListApi} from '../../../api/system';
import Auth from '../../../components/Auth'; // 引入权限控制组件
import { useAuth } from '../../../contexts/AuthContext';
import DictSelect from '../../../components/DictSelect';
import DictTag from '../../../components/DictTag';

export default function MenuManagement() {
    const { user } = useAuth();
    const isSuperAdmin = user?.tenantId === 0; // 判断是否为平台超管

    const [menus, setMenus] = useState([]);
    const [loading, setLoading] = useState(false);

    const [tenants, setTenants] = useState([]);

    const [modalVisible, setModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingMenu, setEditingMenu] = useState(null);
    const [submitLoading, setSubmitLoading] = useState(false);

    const [currentTenantId, setCurrentTenantId] = useState(user?.tenantId); // 当前查看的租户ID

    // 获取菜单列表并构建树
    const fetchMenus = async (tenantId) => {
        setLoading(true);
        try {
            const res = await getMenuListApi({ tenantId });
            setMenus(buildTree(res || []));
        } catch (error) {
            console.error('获取菜单列表失败', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchTenants = async () => {
        // if (isSuperAdmin) {
            const res = await getTenantListApi();
            setTenants(res || []);
        // }
    };

    useEffect(() => {
        fetchTenants();
        fetchMenus(currentTenantId);
    }, []);

    // 将扁平数据转换为树形结构
    const buildTree = (data, parentId = 0) => {
        return data
            .filter(item => item.parentId == parentId)
            .map(item => ({
                ...item,
                children: buildTree(data, item.id)
            }));
    };

    // 保存菜单（新增/编辑）
    const handleSave = async (values) => {
        setSubmitLoading(true);
        try {
            await saveMenuApi({ ...editingMenu, ...values });
            message.success(editingMenu ? '修改成功' : '新增成功');
            setModalVisible(false);
            await fetchMenus(currentTenantId);
        } catch (error) {
            // 错误已在 axios 拦截器中处理
        } finally {
            setSubmitLoading(false);
        }
    };

    // 删除菜单
    const handleDelete = async (id) => {
        try {
            await deleteMenuApi(id);
            message.success('删除成功');
            await fetchMenus(currentTenantId);
        } catch (error) {}
    };

    // 表格列定义
    const columns = [
        { title: '菜单名称', dataIndex: 'name', key: 'name', width: 200 },
        { title: '图标', dataIndex: 'icon', key: 'icon', width: 80 },
        { title: '路由路径', dataIndex: 'path', key: 'path', render: (text) => text ? <code>{text}</code> : '-' },
        {
            title: '权限标识',
            dataIndex: 'perms',
            key: 'perms',
            render: (text) => text ? <Tag color="blue">{text}</Tag> : '-'
        },
        {
            title: '类型',
            dataIndex: 'menuType',
            key: 'menuType',
            width: 80,
            render: (type) => <DictTag dictType="sys_menu_type" value={type} />
        },
        { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
        {
            title: '操作',
            key: 'action',
            width: 220,
            render: (_, record) => (
                <Space>
                    {/* 新增子菜单：需要 sys:menu:add 权限，且只有目录和菜单能新增子节点 */}
                    {record.menuType !== 'F' && (
                        <Auth perm="sys:menu:add">
                            <Button
                                type="link"
                                size="small"
                                icon={<PlusOutlined />}
                                onClick={() => {
                                    setEditingMenu(null);
                                    form.resetFields();
                                    form.setFieldsValue({ parentId: record.id, menuType: 'C', sortOrder: 0 });
                                    setModalVisible(true);
                                }}
                            >
                                新增
                            </Button>
                        </Auth>
                    )}

                    {/* 编辑菜单：需要 sys:menu:edit 权限 */}
                    <Auth perm="sys:menu:edit">
                        <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => {
                                setEditingMenu(record);
                                form.setFieldsValue(record);
                                setModalVisible(true);
                            }}
                        >
                            编辑
                        </Button>
                    </Auth>

                    {/* 删除菜单：需要 sys:menu:remove 权限 */}
                    <Auth perm="sys:menu:remove">
                        <Popconfirm
                            title="确定要删除该菜单吗？"
                            onConfirm={() => handleDelete(record.id)}
                            okText="确定"
                            cancelText="取消"
                        >
                            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    const buildTreeSelectData = (data) => {
        if (!data || data.length === 0) return [];
        return data
            .filter(m => m.menuType !== 'F') // 过滤掉按钮
            .map(m => ({
                key: m.id,        // 显式指定 key，消除 "must have a certain key" 警告
                title: m.name,    // 映射 title
                value: m.id,      // 映射 value
                children: buildTreeSelectData(m.children) // 递归处理子节点
            }));
    };

    const treeSelectData = [
        {
            key: 0,
            title: '顶级菜单',
            value: 0,
            children: buildTreeSelectData(menus) // 使用递归函数处理所有层级
        }
    ];

    // 切换租户时重新加载角色和部门树
    const handleTenantChange = async (tenantId) => {
        setCurrentTenantId(tenantId);
        await fetchMenus(tenantId);
    };


    return (
        <Card
            title="菜单管理"
            extra={
                <Space>
                    {/* 超管视角：租户切换 */}
                    {isSuperAdmin && (
                        <Select
                            style={{ width: 200 }}
                            placeholder="选择租户"
                            value={currentTenantId}
                            onChange={handleTenantChange}
                            options={tenants.map(t => ({ label: t.name, value: t.id }))}
                        />
                    )}
                    <Auth perm="sys:menu:add">
                        <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={() => {
                                setEditingMenu(null);
                                form.resetFields();
                                form.setFieldsValue({ parentId: 0, menuType: 'M', sortOrder: 0 });
                                setModalVisible(true);
                            }}
                        >
                            新增顶级菜单
                        </Button>
                    </Auth>
                </Space>
            }
        >
            <Table
                columns={columns}
                dataSource={menus}
                rowKey="id"
                loading={loading}
                pagination={false}
                defaultExpandAllRows
            />

            {/* 新增/编辑菜单弹窗 */}
            <Modal
                title={editingMenu ? '编辑菜单' : '新增菜单'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                width={600}
                confirmLoading={submitLoading}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    <Form.Item name="parentId" label="上级菜单" rules={[{ required: true, message: '请选择上级菜单' }]}>
                        <TreeSelect
                            treeData={treeSelectData}
                            placeholder="请选择上级菜单"
                            treeDefaultExpandAll
                            showSearch
                            treeNodeFilterProp="title"
                        />
                    </Form.Item>
                    <Form.Item name="menuType" label="菜单类型" rules={[{ required: true, message: '请选择菜单类型' }]}>
                        <DictSelect dictType="sys_menu_type" />
                    </Form.Item>
                    <Form.Item name="name" label="菜单名称" rules={[{ required: true, message: '请输入菜单名称' }]}>
                        <Input placeholder="请输入菜单名称" />
                    </Form.Item>

                    <Form.Item noStyle shouldUpdate={(prevValues, currentValues) => prevValues.menuType !== currentValues.menuType}>
                        {({ getFieldValue }) => {
                            const menuType = getFieldValue('menuType');
                            return menuType !== 'F' ? (
                                <Form.Item name="path" label="路由地址" rules={[{ required: true, message: '请输入路由地址' }]}>
                                    <Input placeholder="如：/system/user" />
                                </Form.Item>
                            ) : null;
                        }}
                    </Form.Item>

                    <Form.Item name="perms" label="权限标识">
                        <Input placeholder="如：sys:user:add（按钮类型必填）" />
                    </Form.Item>

                    <Form.Item name="sortOrder" label="显示排序" rules={[{ required: true, message: '请输入排序' }]}>
                        <InputNumber min={0} style={{ width: '100%' }} placeholder="数字越小越靠前" />
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    );
}
