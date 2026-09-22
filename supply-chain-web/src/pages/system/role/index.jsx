import {useState, useEffect} from 'react';
import {
    Table, Button, Space, Card, Modal, Form, Input, Select, TreeSelect,
    message, Popconfirm, Tree, Spin, Tag
} from 'antd';
import {PlusOutlined, SettingOutlined, EditOutlined, DeleteOutlined} from '@ant-design/icons';
import {
    getRoleListApi, saveRoleApi, deleteRoleApi,
    getMenuListApi, getRoleMenusApi, assignRoleMenusApi,
    getTenantListApi, getDeptTreeApi
} from '../../../api/system';
import Auth from '../../../components/Auth';
import {useAuth} from '../../../contexts/AuthContext';
import DictSelect from '../../../components/DictSelect';
import DictTag from '../../../components/DictTag';

export default function RoleManagement() {
    const {user} = useAuth();
    const isSuperAdmin = user?.tenantId === 0; // 判断是否为平台超管

    const [roles, setRoles] = useState([]);
    const [tenants, setTenants] = useState([]);
    const [deptTree, setDeptTree] = useState([]);
    const [loading, setLoading] = useState(false);
    const [currentTenantId, setCurrentTenantId] = useState(user?.tenantId); // 当前查看的租户ID

    // 角色表单弹窗状态
    const [modalVisible, setModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingRole, setEditingRole] = useState(null);
    const [submitLoading, setSubmitLoading] = useState(false);

    // 分配权限弹窗状态
    const [permModalVisible, setPermModalVisible] = useState(false);
    const [menuTree, setMenuTree] = useState([]);
    const [checkedKeys, setCheckedKeys] = useState([]);
    const [currentRoleId, setCurrentRoleId] = useState(null);
    const [treeLoading, setTreeLoading] = useState(false);

    // 数据范围映射配置
    const dataScopeOptions = [
        {value: 1, label: '全部数据', color: 'red'},
        {value: 2, label: '本部门及以下数据', color: 'orange'},
        {value: 3, label: '本部门数据', color: 'blue'},
        {value: 4, label: '仅本人数据', color: 'green'},
        {value: 5, label: '自定义数据', color: 'purple'}
    ];

    // ================= 数据获取逻辑 =================

    const fetchRoles = async (tenantId) => {
        setLoading(true);
        try {
            const res = await getRoleListApi({tenantId});
            setRoles(res || []);
        } finally {
            setLoading(false);
        }
    };

    const fetchTenants = async () => {
        if (isSuperAdmin) {
            const res = await getTenantListApi();
            setTenants(res || []);
        }
    };

    const fetchDeptTree = async (tenantId) => {
        // if (!tenantId) {
        //     ([]);
        //     return;
        // }
        const res = await getDeptTreeApi({tenantId});
        setDeptTree(res || []);
    };

    useEffect(() => {
        fetchTenants();
        fetchRoles(currentTenantId);
        // 普通管理员初始化时加载自己的部门树setDeptTree
        // if (!isSuperAdmin) {
        fetchDeptTree(user?.tenantId);
        // }
    }, []);

    // 切换租户时重新加载角色和部门树
    const handleTenantChange = async (tenantId) => {
        setCurrentTenantId(tenantId);
        await fetchRoles(tenantId);
        await fetchDeptTree(tenantId);
    };

    // ================= 角色 CRUD 逻辑 =================

    const handleSave = async (values) => {
        setSubmitLoading(true);
        try {
            // 如果是超管新增，带上选中的租户ID
            if (isSuperAdmin && !editingRole) {
                values.tenantId = currentTenantId;
            }
            await saveRoleApi({...editingRole, ...values});
            message.success(editingRole ? '修改成功' : '新增成功');
            setModalVisible(false);
            fetchRoles(currentTenantId);
        } finally {
            setSubmitLoading(false);
        }
    };

    const handleDelete = async (id) => {
        try {
            await deleteRoleApi(id);
            message.success('删除成功');
            fetchRoles(currentTenantId);
        } catch (error) {
        }
    };

    const openAddModal = async () => {
        setEditingRole(null);
        form.resetFields();
        setModalVisible(true);
        // 超管新增时不默认加载部门树（等选了租户再加载），普通管理员加载当前租户部门树
        if (!isSuperAdmin) {
            await fetchDeptTree(user?.tenantId);
        } else {
            setDeptTree([]);
        }
    };

    const openEditModal = async (record) => {
        setEditingRole(record);
        // 【优化】：使用 ?? 代替 ||，防止 dataScope 为 0 时被错误替换为 4
        form.setFieldsValue({...record, dataScope: record.dataScope ?? 4});
        setModalVisible(true);
        // 加载该角色所属租户的部门树
        await fetchDeptTree(record.tenantId);
    };

    // ================= 分配菜单权限逻辑 =================

    const openPermModal = async (record) => {
        setCurrentRoleId(record.id);
        setTreeLoading(true);
        setPermModalVisible(true);
        try {
            const [allMenusRes, roleMenusRes] = await Promise.all([getMenuListApi({tenantId: currentTenantId}), getRoleMenusApi(record.id, currentTenantId)]);
            setMenuTree(buildMenuTree(allMenusRes || []));
            setCheckedKeys(getLeafKeys(allMenusRes || [], roleMenusRes || []));
        } finally {
            setTreeLoading(false);
        }
    };

    const buildMenuTree = (menus, parentId = 0) => menus
        .filter(m => m.parentId == parentId)
        .map(m => ({title: m.name, key: m.id, children: buildMenuTree(menus, m.id)}));

    const getLeafKeys = (allMenus, checkedIds) => {
        const parentIds = allMenus.map(m => m.parentId).filter(id => id != 0);
        return checkedIds.filter(id => !parentIds.includes(id) || !allMenus.find(m => m.id == id));
    };

    const handleAssignPerms = async () => {
        try {
            await assignRoleMenusApi(currentRoleId, currentTenantId, checkedKeys);
            message.success('权限分配成功');
            setPermModalVisible(false);
        } catch (error) {
        }
    };

    // ================= 表格列定义 =================

    const columns = [
        {title: '角色名称', dataIndex: 'name', key: 'name'},
        {title: '角色编码', dataIndex: 'code', key: 'code', render: (text) => <code>{text}</code>},
        {
            title: '数据范围',
            dataIndex: 'dataScope',
            key: 'dataScope',
            render: (scope) => <DictTag dictType="sys_data_scope" value={scope}/>
        },
        {
            title: '操作',
            key: 'action',
            width: 280,
            render: (_, record) => (
                <Space>
                    <Auth perm="sys:role:edit">
                        <Button type="link" icon={<SettingOutlined/>}
                                onClick={() => openPermModal(record)}>分配菜单</Button>
                    </Auth>
                    <Auth perm="sys:role:edit">
                        <Button type="link" icon={<EditOutlined/>} onClick={() => openEditModal(record)}>编辑</Button>
                    </Auth>
                    <Auth perm="sys:role:remove">
                        <Popconfirm title="确定要删除该角色吗？" onConfirm={() => handleDelete(record.id)}>
                            <Button type="link" danger icon={<DeleteOutlined/>}>删除</Button>
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    // ================= 渲染 UI =================

    // 【新增】：转换函数
    const convertToTreeSelectData = (nodes) => {
        if (!nodes || nodes.length === 0) return [];
        return nodes.map(node => ({
            title: node.name,
            value: node.id,
            key: node.id,
            children: convertToTreeSelectData(node.children)
        }));
    };

    return (
        <Card
            title="角色管理"
            extra={
                <Space>
                    {/* 超管视角：租户切换 */}
                    {isSuperAdmin && (
                        <Select
                            style={{width: 200}}
                            placeholder="选择租户"
                            value={currentTenantId}
                            onChange={handleTenantChange}
                            options={tenants.map(t => ({label: t.name, value: t.id}))}
                        />
                    )}
                    <Auth perm="sys:role:add">
                        <Button type="primary" icon={<PlusOutlined/>} onClick={openAddModal}>
                            新增角色
                        </Button>
                    </Auth>
                </Space>
            }
        >
            <Table columns={columns} dataSource={roles} rowKey="id" loading={loading} pagination={false}/>

            {/* 新增/编辑角色弹窗 */}
            <Modal
                title={editingRole ? '编辑角色' : '新增角色'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                confirmLoading={submitLoading}
                destroyOnHidden
                width={600}
            >
                <Form form={form} layout="vertical" onFinish={handleSave}>

                    {/* 超管新增时选择租户 */}
                    {isSuperAdmin && !editingRole && (
                        <Form.Item name="tenantId" label="所属租户"
                                   rules={[{required: true, message: '请选择所属租户'}]}>
                            <Select
                                placeholder="请选择所属租户"
                                options={tenants.map(t => ({label: t.name, value: t.id}))}
                                onChange={(val) => {
                                    setCurrentTenantId(val);
                                    fetchDeptTree(val);
                                    form.setFieldValue('deptId', null); // 切换租户时清空已选部门
                                }}
                            />
                        </Form.Item>
                    )}

                    <Form.Item name="name" label="角色名称" rules={[{required: true, message: '请输入角色名称'}]}>
                        <Input placeholder="请输入角色名称"/>
                    </Form.Item>

                    <Form.Item name="code" label="角色编码" rules={[{required: true, message: '请输入角色编码'}]}>
                        <Input placeholder="请输入角色编码（如：ROLE_ADMIN）" disabled={!!editingRole}/>
                    </Form.Item>

                    {/* 归属部门选择 */}
                    <Form.Item
                        name="deptId"
                        label="归属部门"
                        tooltip="用于数据权限计算或角色默认归属，不选则不限制"
                    >
                        <TreeSelect
                            treeData={convertToTreeSelectData(deptTree)}
                            placeholder="请选择归属部门(可选)"
                            treeDefaultExpandAll
                            allowClear
                            showSearch
                            treeNodeFilterProp="title"
                        />
                    </Form.Item>

                    {/* 数据范围选择 */}
                    <Form.Item
                        name="dataScope"
                        label="数据范围"
                        rules={[{required: true, message: '请选择数据范围'}]}
                        initialValue={4}
                        tooltip="控制该角色下的用户能看到哪些业务数据（如订单、合同等）"
                    >
                        <DictSelect dictType="sys_data_scope"/>
                    </Form.Item>

                </Form>
            </Modal>

            {/* 分配菜单权限弹窗 */}
            <Modal
                title="分配菜单权限"
                open={permModalVisible}
                onCancel={() => setPermModalVisible(false)}
                onOk={handleAssignPerms}
                width={600}
                destroyOnHidden
            >
                <Spin spinning={treeLoading}>
                    <div style={{
                        maxHeight: 400,
                        overflowY: 'auto',
                        border: '1px solid #f0f0f0',
                        padding: 16,
                        borderRadius: 4
                    }}>
                        <Tree
                            checkable
                            defaultExpandAll
                            checkedKeys={checkedKeys}
                            onCheck={(keys) => setCheckedKeys(keys)}
                            treeData={menuTree}
                        />
                    </div>
                </Spin>
            </Modal>
        </Card>
    );
}
