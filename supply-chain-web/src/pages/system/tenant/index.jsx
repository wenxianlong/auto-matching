import { useState, useEffect } from 'react';
import { Card, Table, Button, Space, Modal, Form, Input, Select, Tag, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons';
import Auth from '../../../components/Auth';
import CategoryAuthModal from './CategoryAuthModal';
import { getTenantListApi, addTenantApi, updateTenantApi, deleteTenantApi } from '../../../api/system';

export default function TenantManagement() {
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);

    // 租户表单弹窗状态
    const [editingTenant, setEditingTenant] = useState(null);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [form] = Form.useForm();

    // 品类授权弹窗状态
    const [authModalOpen, setAuthModalOpen] = useState(false);
    const [currentAuthTenant, setCurrentAuthTenant] = useState(null);

    // ================= 1. 数据获取 =================
    const fetchTenants = async () => {
        setLoading(true);
        try {
            const res = await getTenantListApi();
            setTenants(res || []);
        } catch (error) {
            message.error('获取租户列表失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchTenants(); }, []);

    // ================= 2. 租户 CRUD 操作 =================
    const openTenantModal = (record = null) => {
        setEditingTenant(record);
        form.resetFields();
        if (record) {
            form.setFieldsValue(record);
        } else {
            form.setFieldsValue({ status: 1 });
        }
        setModalVisible(true);
    };

    const handleSaveTenant = async (values) => {
        setSubmitLoading(true);
        try {
            if (editingTenant) {
                await updateTenantApi({ ...editingTenant, ...values });
                message.success('修改成功');
            } else {
                await addTenantApi(values);
                message.success('新增成功');
            }
            setModalVisible(false);
            fetchTenants();
        } catch (error) {
            message.error(error?.response?.data?.msg || '保存失败');
        } finally {
            setSubmitLoading(false);
        }
    };

    const handleDeleteTenant = async (id) => {
        try {
            await deleteTenantApi(id);
            message.success('删除成功');
            fetchTenants();
        } catch (error) {
            message.error('删除失败');
        }
    };

// ================= 3. 品类授权操作 =================
    const openAuthModal = (tenant) => {
        setCurrentAuthTenant(tenant);
        setAuthModalOpen(true);
    };

    // ================= 4. 表格列定义 =================
    const columns = [
        {
            title: '租户名称',
            dataIndex: 'name',
            key: 'name',
            width: 200
        },
        {
            title: '租户编码',
            dataIndex: 'code',
            key: 'code',
            width: 150
        },
        {
            title: '联系人',
            dataIndex: 'contactUser',
            key: 'contactUser',
            width: 120
        },
        {
            title: '联系电话',
            dataIndex: 'contactPhone',
            key: 'contactPhone',
            width: 150
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (status) => (
                <Tag color={status === 1 ? 'success' : 'danger'}>
                    {status === 1 ? '正常' : '停用'}
                </Tag>
            )
        },
        {
            title: '操作',
            key: 'action',
            width: 260,
            fixed: 'right',
            render: (_, record) => (
                <Space size="small">
                    {/* 分配品类按钮 */}
                    {record.id > 0 && (
                        <Auth perm="business:tenant:config">
                            <Button
                                type="link"
                                size="small"
                                icon={<SettingOutlined />}
                                onClick={() => openAuthModal(record)}
                            >
                                分配品类
                            </Button>
                        </Auth>
                    )}

                    {/* 编辑按钮 */}
                    <Auth perm="sys:tenant:edit">
                        <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => openTenantModal(record)}
                        />
                    </Auth>

                    {/* 删除按钮 (禁止删除超级租户/平台自身) */}
                    <Auth perm="sys:tenant:remove">
                        <Popconfirm
                            title="确定删除该租户吗？"
                            description="删除后该租户下所有数据将不可恢复！"
                            onConfirm={() => handleDeleteTenant(record.id)}
                            okText="确定删除"
                            cancelText="取消"
                            okButtonProps={{ danger: true }}
                        >
                            <Button
                                type="link"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                                disabled={record.id === 0 || record.code === 'PLATFORM'}
                            />
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    // ================= 5. 渲染 UI =================
    return (
        <Card
            title="租户管理"
            extra={
                <Auth perm="sys:tenant:add">
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => openTenantModal()}>
                        新增租户
                    </Button>
                </Auth>
            }
        >
            <Table
                columns={columns}
                dataSource={tenants}
                rowKey="id"
                loading={loading}
                pagination={false}
                scroll={{ x: 1000 }}
                size="middle"
            />

            {/* 新增/编辑租户弹窗 */}
            <Modal
                title={editingTenant ? '编辑租户' : '新增租户'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                confirmLoading={submitLoading}
                destroyOnHidden
                width={500}
            >
                <Form form={form} layout="vertical" onFinish={handleSaveTenant}>
                    <Form.Item name="name" label="租户名称" rules={[{ required: true, message: '请输入租户名称' }]}>
                        <Input placeholder="如：深圳某某科技有限公司" />
                    </Form.Item>
                    <Form.Item name="code" label="租户编码" rules={[{ required: true, message: '请输入租户编码' }]}>
                        <Input placeholder="如：SZ_TECH" disabled={!!editingTenant} />
                    </Form.Item>
                    <Form.Item name="contactUser" label="联系人">
                        <Input placeholder="请输入联系人姓名" />
                    </Form.Item>
                    <Form.Item name="contactPhone" label="联系电话">
                        <Input placeholder="请输入手机号码" />
                    </Form.Item>
                    <Form.Item name="status" label="状态" initialValue={1}>
                        <Select options={[
                            { value: 1, label: '正常' },
                            { value: 0, label: '停用' }
                        ]} />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 品类授权弹窗 */}
            <CategoryAuthModal
                open={authModalOpen}
                tenant={currentAuthTenant}
                onCancel={() => setAuthModalOpen(false)}
                onSuccess={() => {
                    // 授权成功后可选择是否刷新列表
                    // fetchTenants();
                }}
            />
        </Card>
    );
}
