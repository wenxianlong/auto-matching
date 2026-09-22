import {useState, useEffect} from 'react';
import {Table, Button, Space, Card, Modal, Form, Input, Select, message, Popconfirm, Tag, TreeSelect} from 'antd';
import {PlusOutlined, UserSwitchOutlined} from '@ant-design/icons';
import {
    getUserPageApi, saveUserApi, deleteUserApi,
    getRoleListApi, getUserRolesApi, assignUserRolesApi,
    getTenantListApi, getDeptTreeApi, getDeptListApi // 引入租户列表接口
} from '../../../api/system';
import Auth from '../../../components/Auth';
import DictSelect from '../../../components/DictSelect';
import DictTag from '../../../components/DictTag';
import {useAuth} from "../../../contexts/AuthContext.jsx";

export default function UserManagement() {
    const {user} = useAuth();
    const isSuperAdmin = user?.tenantId === 0; // 判断是否为平台超管
    const [users, setUsers] = useState([]);
    const [roles, setRoles] = useState([]);
    const [tenants, setTenants] = useState([]); // 租户列表
    //部门信息
    const [deptTree, setDeptTree] = useState([]);
    const [deptList, setDeptList] = useState([]); // 租户列表


    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({current: 1, pageSize: 10, total: 0});

    const [modalVisible, setModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingUser, setEditingUser] = useState(null);

    // 分配角色弹窗状态 (保持不变)
    const [roleModalVisible, setRoleModalVisible] = useState(false);
    const [currentUserId, setCurrentUserId] = useState(null);
    const [currentTenantId, setCurrentTenantId] = useState(user?.tenantId); // 当前查看的租户ID
    const [selectedRoleIds, setSelectedRoleIds] = useState([]);

    // 获取基础数据
    const fetchInitData = async (tenantId) => {
        const [tenantRes, roleRes, deptList] = await Promise.all([
            getTenantListApi(),
            getRoleListApi(tenantId), // 初始加载当前租户的角色
            getDeptListApi(tenantId)
        ]);
        setTenants(tenantRes || []);
        setRoles(roleRes || []);
        setDeptList(deptList || [])
    };

    const fetchData = async (params = {}) => {
        setLoading(true);
        try {
            const res = await getUserPageApi({
                pageNum: params.current || 1,
                pageSize: params.pageSize || 10,
                tenantId: params.tenantId
            });
            setUsers(res.records || []);
            setPagination({...pagination, current: res.current, pageSize: res.size, total: res.total});
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchInitData({tenantId: currentTenantId});
        fetchData({tenantId: currentTenantId});
    }, []);

    // 【核心】：当选择的租户发生变化时，重新获取该租户下的角色
    const handleTenantChange = async (tenantId) => {
        form.setFieldsValue({roleIds: []}); // 清空已选角色
        setCurrentTenantId(tenantId);
        await fetchDeptList(tenantId)
        await fetchRoles(tenantId);
        await fetchDeptTree(tenantId);
        await fetchData({tenantId: tenantId})
    };

    const fetchRoles = async (tenantId) => {
        setLoading(true);
        try {
            const res = await getRoleListApi({tenantId});
            setRoles(res || []);
        } finally {
            setLoading(false);
        }
    };

    //切换部门树
    const fetchDeptTree = async (tenantId) => {
        setCurrentTenantId(tenantId);
        form.setFieldValue('deptId', null); // 切换租户时清空已选部门
        const res = await getDeptTreeApi({tenantId});
        setDeptTree(res || []);
    };


    //切换部门列表
    const fetchDeptList = async (tenantId) => {
        setCurrentTenantId(tenantId);
        console.log("tenantId===="+tenantId);
        form.setFieldValue('deptId', null); // 切换租户时清空已选部门
        const res = await getDeptListApi({tenantId:tenantId});
        setDeptList(res || [])
    };

    const handleSave = async (values) => {
        await saveUserApi({...editingUser, ...values});
        message.success('保存成功');
        setModalVisible(false);
        await fetchData({current: pagination.current});
    };

    // 打开编辑弹窗时，回显数据并加载对应租户的角色
    const openEditModal = async (record) => {
        setEditingUser(record);
        await fetchDeptTree(record.tenantId);

        // 2. 加载该用户所属租户的所有角色，用于下拉框选项
        const roleRes = await getRoleListApi({ tenantId: record.tenantId });
        setRoles(roleRes || []);
        // 3. 获取用户已有的角色ID列表，并转换为Select组件需要的对象格式
        // 这是解决“分配角色没有带出来”的关键步骤
        const userRoleIds = await getUserRolesApi(record.id);
        const userRoles = (roleRes || [])
            .filter(role => userRoleIds.includes(role.id))
            .map(role => ({ label: role.name, value: role.id }));
        form.setFieldsValue({
            ...record,
            roleIds: userRoles
        });

        setModalVisible(true);
        // 加载该用户所属租户的角色
        if (record.tenantId) {
            const res = await getRoleListApi({tenantId: record.tenantId});
            setRoles(res || []);
        }
    };

    // 分配角色相关逻辑 (保持不变)
    const openRoleModal = async (record) => {
        setCurrentUserId(record.id);
        const res = await getUserRolesApi(record.id);
        setSelectedRoleIds(res || []);
        setRoleModalVisible(true);
    };

    const handleAssignRoles = async () => {
        await assignUserRolesApi(currentUserId, currentTenantId, selectedRoleIds);
        message.success('角色分配成功');
        setRoleModalVisible(false);
        await fetchData({current: pagination.current, tenantId: currentTenantId});
    };

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

    const columns = [
        {title: '用户名', dataIndex: 'username', key: 'username'},
        {title: '昵称', dataIndex: 'nickname', key: 'nickname'},
        // {
        //     title: '所属租户', dataIndex: 'tenantId', key: 'tenantId',
        //     render: (id) => {
        //         if (id === 0) return <Tag color="gold">平台管理中心</Tag>;
        //         const tenant = tenants.find(t => t.id === id);
        //         return tenant ? <Tag color="blue">{tenant.name}</Tag> : `ID: ${id}`;
        //     }
        // },
        {
            title: '部门', dataIndex: 'deptId', key: 'deptId',
            render: (id) => {
                // if (id === 0) return <Tag color="gold">平台管理</Tag>;
                const dept = deptList.find(t => t.id === id);
                return dept ? <Tag color="yellow">{dept.name}</Tag> : `ID: ${id}`;
            }
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (s) => s === 1 ? <Tag color="green">正常</Tag> : <Tag color="red">停用</Tag>
        },
        {
            title: '操作', key: 'action', width: 250, render: (_, record) => (
                <Space>
                    <Auth perm="sys:user:edit">
                        <Button type="link" icon={<UserSwitchOutlined/>}
                                onClick={() => openRoleModal(record)}>分配角色</Button>
                    </Auth>
                    <Auth perm="sys:user:edit">
                        <Button type="link" onClick={() => openEditModal(record)}>编辑</Button>
                    </Auth>
                    <Auth perm="sys:user:remove">
                        <Popconfirm title="确定删除?" onConfirm={async () => {
                            await deleteUserApi(record.id);
                            message.success('删除成功');
                            fetchData(currentTenantId);
                        }}>
                            <Button type="link" danger>删除</Button>
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    return (
        <Card
            title="用户管理"
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
                    <Auth perm="sys:user:add">
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => {
                            setEditingUser(null);
                            form.resetFields();
                            setModalVisible(true);
                            // 新增时，默认加载当前登录用户所在租户的角色
                            getRoleListApi().then(res => setRoles(res || []));
                        }}>
                            新增用户
                        </Button>
                    </Auth>
                </Space>

            }
        >
            <Table columns={columns} dataSource={users} rowKey="id" loading={loading} pagination={pagination}
                   onChange={fetchData}/>

            {/* 新增/编辑用户弹窗 */}
            <Modal title={editingUser ? '编辑用户' : '新增用户'} open={modalVisible}
                   onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnHidden>
                <Form form={form} layout="vertical" onFinish={handleSave}>

                    {/* 【新增】：所属租户选择 (仅超级管理员或新增时显示) */}
                    <Form.Item name="tenantId" label="所属租户" rules={[{required: true, message: '请选择所属租户'}]}>
                        <Select
                            placeholder="请选择所属租户"
                            disabled={!!editingUser} // 编辑时不允许修改租户
                            onChange={handleTenantChange}
                            options={tenants.map(t => ({label: t.name, value: t.id}))}
                        />
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

                    <Form.Item name="username" label="用户名" rules={[{required: true}]}>
                        <Input disabled={!!editingUser}/>
                    </Form.Item>

                    {!editingUser && (
                        <Form.Item name="password" label="密码" rules={[{required: true}]}>
                            <Input.Password/>
                        </Form.Item>
                    )}

                    <Form.Item name="nickname" label="昵称">
                        <Input/>
                    </Form.Item>

                    <Form.Item name="roleIds" label="分配角色">
                        <Select
                            mode="multiple"
                            labelInValue={true}
                            placeholder="请先选择所属租户"
                            options={roles.map(r => ({label: r.name, value: r.id}))}
                        />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 分配角色弹窗 (保持不变) */}
            <Modal title="分配角色" open={roleModalVisible} onCancel={() => setRoleModalVisible(false)}
                   onOk={handleAssignRoles}>
                <Select
                    mode="multiple"
                    style={{width: '100%'}}
                    placeholder="请选择角色"
                    value={selectedRoleIds}
                    onChange={setSelectedRoleIds}
                    options={roles.map(r => ({label: r.name, value: r.id}))}
                />
            </Modal>
        </Card>
    );
}
