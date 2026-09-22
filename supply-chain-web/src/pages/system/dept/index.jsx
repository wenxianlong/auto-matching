import { useState, useEffect } from 'react';
import { Table, Button, Space, Card, Modal, Form, Input, TreeSelect, message, Popconfirm, Tag, InputNumber, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getDeptListApi, saveDeptApi, deleteDeptApi, getTenantListApi } from '../../../api/system';
import Auth from '../../../components/Auth';
import { useAuth } from '../../../contexts/AuthContext';
import DictSelect from '../../../components/DictSelect';
import DictTag from '../../../components/DictTag';

export default function DeptManagement() {
    const { user } = useAuth();
    const isSuperAdmin = user?.tenantId === 0; // 判断是否为平台超管

    const [depts, setDepts] = useState([]);
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(false);

    const [modalVisible, setModalVisible] = useState(false);
    const [form] = Form.useForm();
    const [editingDept, setEditingDept] = useState(null);
    const [currentTenantId, setCurrentTenantId] = useState(user?.tenantId); // 当前操作的租户ID

    const fetchData = async (tenantId) => {
        setLoading(true);
        try {
            const res = await getDeptListApi({ tenantId });
            setDepts(buildTree(res || []));
        } finally { setLoading(false); }
    };

    const fetchTenants = async () => {
        if (isSuperAdmin) {
            const res = await getTenantListApi();
            setTenants(res || []);
        }
    };

    useEffect(() => {
        fetchTenants();
        fetchData(currentTenantId);
    }, []);

    // 切换租户时重新加载部门
    const handleTenantChange = (tenantId) => {
        setCurrentTenantId(tenantId);
        fetchData(tenantId);
    };

    const buildTree = (data, parentId = 0) => data
        .filter(item => item.parentId == parentId)
        .map(item => ({ ...item, children: buildTree(data, item.id) }));

    const handleSave = async (values) => {
        // 超管新增时，带上选中的租户ID
        if (isSuperAdmin && !editingDept) {
            values.tenantId = currentTenantId;
        }
        await saveDeptApi({ ...editingDept, ...values });
        message.success('保存成功');
        setModalVisible(false);
        fetchData(currentTenantId);
    };

    const columns = [
        { title: '部门名称', dataIndex: 'name', key: 'name', width: 250 },
        { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
        { title: '负责人', dataIndex: 'leader', key: 'leader', width: 120 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 80,
            // render: (status) => <DictTag dictType="sys_status" value={status} />
            render: (status) => (
                <Tag color={status === 1 ? 'success' : 'danger'}>
                    {status === 1 ? '正常' : '停用'}
                </Tag>
            )
        },
        {
            title: '操作', key: 'action', width: 200, render: (_, record) => (
                <Space>
                    <Auth perm="sys:dept:add">
                        <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => {
                            setEditingDept(null); form.resetFields();
                            form.setFieldsValue({ parentId: record.id, sortOrder: 0, status: 1, tenantId: currentTenantId });
                            setModalVisible(true);
                        }}>新增</Button>
                    </Auth>
                    <Auth perm="sys:dept:edit">
                        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => {
                            setEditingDept(record); form.setFieldsValue(record); setModalVisible(true);
                        }}>编辑</Button>
                    </Auth>
                    <Auth perm="sys:dept:remove">
                        <Popconfirm title="确定删除?" onConfirm={async () => { await deleteDeptApi(record.id); fetchData(currentTenantId); }}>
                            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    // 【新增】：将 Table 用的树形数据转换为 TreeSelect 需要的格式
    const convertToTreeSelectData = (nodes) => {
        if (!nodes || nodes.length === 0) return [];
        return nodes
            .filter(node => node.id !== undefined && node.id !== null) // 过滤掉 id 为空的节点
            .map(node => ({
                title: node.name,
                value: String(node.id), // 强制转为 String
                key: String(node.id),
                children: node.children ? convertToTreeSelectData(node.children) : []
            }));
    };

    // 【修改】：构建 TreeSelect 数据时，使用转换后的数据
    const treeData = [
        {
            title: '顶级部门',
            value: 0,
            key: 0,
            children: convertToTreeSelectData(depts) // 这里传入转换后的数据
        }
    ];

    return (
        <Card
            title="部门管理"
            extra={
                <Space>
                    {isSuperAdmin && (
                        <Select
                            style={{ width: 200 }}
                            placeholder="选择租户"
                            value={currentTenantId}
                            onChange={handleTenantChange}
                            options={tenants.map(t => ({ label: t.name, value: t.id }))}
                        />
                    )}
                    <Auth perm="sys:dept:add">
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => {
                            setEditingDept(null); form.resetFields();
                            form.setFieldsValue({ parentId: 0, sortOrder: 0, status: 1, tenantId: currentTenantId });
                            setModalVisible(true);
                        }}>新增顶级部门</Button>
                    </Auth>
                </Space>
            }
        >
            <Table columns={columns} dataSource={depts} rowKey="id" loading={loading} pagination={false} defaultExpandAllRows />

            <Modal title={editingDept ? '编辑部门' : '新增部门'} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} destroyOnHidden>
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    {isSuperAdmin && !editingDept && (
                        <Form.Item name="tenantId" label="所属租户" rules={[{ required: true }]}>
                            <Select disabled options={tenants.map(t => ({ label: t.name, value: t.id }))} />
                        </Form.Item>
                    )}
                    <Form.Item name="parentId" label="上级部门" rules={[{ required: true }]}>
                        <TreeSelect treeData={treeData} placeholder="请选择上级部门" treeDefaultExpandAll />
                    </Form.Item>
                    <Form.Item name="name" label="部门名称" rules={[{ required: true }]}><Input /></Form.Item>
                    <Form.Item name="sortOrder" label="显示排序" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
                    <Form.Item name="leader" label="负责人"><Input /></Form.Item>
                    <Form.Item name="status" label="状态" rules={[{ required: true }]}>
                        <DictSelect dictType="sys_status" />
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    );
}
