import { useState, useEffect } from 'react';
import { Row, Col, Card, Table, Button, Space, Modal, Form, Input, InputNumber, Select, message, Popconfirm, Tag } from 'antd';
import { useAuth } from '../../../contexts/AuthContext';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import request from '../../../api/request';
import Auth from '../../../components/Auth';

export default function DictManagement() {
    const { user } = useAuth();
    // 左侧：字典类型
    const [types, setTypes] = useState([]);
    const [typeLoading, setTypeLoading] = useState(false);
    const [selectedType, setSelectedType] = useState(null);

    // 右侧：字典数据
    const [datas, setDatas] = useState([]);
    const [dataLoading, setDataLoading] = useState(false);

    // 弹窗状态
    const [modalVisible, setModalVisible] = useState(false);
    const [modalType, setModalType] = useState('type'); // 'type' 或 'data'
    const [editingRecord, setEditingRecord] = useState(null);
    const [form] = Form.useForm();

    // ================= 数据获取 =================
    const fetchTypes = async () => {
        setTypeLoading(true);
        try {
            const res = await request.get('/admin/system/dict/type/list');
            setTypes(res || []);
        } finally { setTypeLoading(false); }
    };

    const fetchDatas = async (dictType) => {
        if (!dictType) { setDatas([]); return; }
        setDataLoading(true);
        try {
            const res = await request.get('/admin/system/dict/data/list', { params: { dictType } });
            setDatas(res || []);
        } finally { setDataLoading(false); }
    };

    useEffect(() => { fetchTypes(); }, []);

    // ================= 交互逻辑 =================
    const handleSelectType = (record) => {
        setSelectedType(record);
        fetchDatas(record.dictType);
    };

    const openModal = (type, record = null) => {
        setModalType(type);
        setEditingRecord(record);
        form.resetFields();
        if (record) {
            form.setFieldsValue(record);
        } else if (type === 'data' && selectedType) {
            form.setFieldsValue({ dictType: selectedType.dictType, sortOrder: 0, status: 1 });
        }
        setModalVisible(true);
    };

    const handleSave = async (values) => {
        const url = modalType === 'type' ? '/admin/system/dict/type' : '/admin/system/dict/data';
        if (editingRecord) {
            await request.put(url, { ...editingRecord, ...values });
            message.success('修改成功');
        } else {
            await request.post(url, values);
            message.success('新增成功');
        }
        setModalVisible(false);
        if (modalType === 'type') fetchTypes();
        else fetchDatas(selectedType.dictType);
    };

    const handleDelete = async (type, id) => {
        const url = modalType === 'type' ? `/admin/system/dict/type/${id}` : `/admin/system/dict/data/${id}`;
        await request.delete(url);
        message.success('删除成功');
        if (type === 'type') {
            fetchTypes();
            if (selectedType && selectedType.id === id) {
                setSelectedType(null);
                setDatas([]);
            }
        } else {
            fetchDatas(selectedType.dictType);
        }
    };

    // ================= 表格列定义 =================
    const typeColumns = [
        { title: '字典名称', dataIndex: 'dictName', key: 'dictName' },
        { title: '字典类型', dataIndex: 'dictType', key: 'dictType', render: t => <code>{t}</code> },
        { title: '状态', dataIndex: 'status', key: 'status', render: s => s === 1 ? <Tag color="success">正常</Tag> : <Tag color="danger">停用</Tag> },
        {
            title: '操作', key: 'action', width: 120,
            render: (_, record) => {
                // 如果是公共字典(tenant_id=0) 且 当前用户不是超管，则隐藏编辑和删除按钮
                const isPublic = record.tenantId === 0 || record.tenantId === '0';
                const isSuperAdmin = user?.tenantId === 0;
                if (isPublic && !isSuperAdmin) return <Tag>系统内置</Tag>;

                return (
                    <Space>
                        <Auth perm="sys:dict:edit"><Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal('type', record)} /></Auth>
                        <Auth perm="sys:dict:remove">
                            <Popconfirm title="确定删除?" onConfirm={() => handleDelete('type', record.id)}>
                                <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                            </Popconfirm>
                        </Auth>
                    </Space>
                );
            }
        }
    ];

    const dataColumns = [
        { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
        { title: '字典键值', dataIndex: 'dictValue', key: 'dictValue' },
        { title: '回显样式', dataIndex: 'listClass', key: 'listClass', render: c => c ? <Tag color={c}>{c}</Tag> : '-' },
        { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
        {
            title: '操作', key: 'action', width: 120,
            render: (_, record) => {
                const isPublic = record.tenantId === 0 || record.tenantId === '0';
                const isSuperAdmin = user?.tenantId === 0;
                if (isPublic && !isSuperAdmin) return <Tag>系统内置</Tag>;
                return (
                    <Space>
                        <Auth perm="sys:dict:edit"><Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal('data', record)} /></Auth>
                        <Auth perm="sys:dict:remove">
                            <Popconfirm title="确定删除?" onConfirm={() => handleDelete('data', record.id)}>
                                <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                            </Popconfirm>
                        </Auth>
                    </Space>
                );
            }
        }
    ];

    return (
        <Row gutter={16}>
            {/* 左侧：字典类型 */}
            <Col span={10}>
                <Card
                    title="字典类型"
                    extra={
                        <Auth perm="sys:dict:add">
                            <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal('type')}>新增类型</Button>
                        </Auth>
                    }
                >
                    <Table
                        columns={typeColumns}
                        dataSource={types}
                        rowKey="id"
                        loading={typeLoading}
                        pagination={false}
                        size="small"
                        onRow={(record) => ({
                            onClick: () => handleSelectType(record),
                            style: { cursor: 'pointer', background: selectedType?.id === record.id ? '#e6f7ff' : 'transparent' }
                        })}
                    />
                </Card>
            </Col>

            {/* 右侧：字典数据 */}
            <Col span={14}>
                <Card
                    title={`字典数据 ${selectedType ? `[${selectedType.dictName}]` : ''}`}
                    extra={
                        <Auth perm="sys:dict:add">
                            <Button type="primary" icon={<PlusOutlined />} disabled={!selectedType} onClick={() => openModal('data')}>新增数据</Button>
                        </Auth>
                    }
                >
                    <Table
                        columns={dataColumns}
                        dataSource={datas}
                        rowKey="id"
                        loading={dataLoading}
                        pagination={false}
                        size="small"
                    />
                </Card>
            </Col>

            {/* 新增/编辑弹窗 */}
            <Modal
                title={`${editingRecord ? '编辑' : '新增'}${modalType === 'type' ? '字典类型' : '字典数据'}`}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" onFinish={handleSave}>
                    {modalType === 'type' ? (
                        <>
                            <Form.Item name="dictName" label="字典名称" rules={[{ required: true }]}><Input /></Form.Item>
                            <Form.Item name="dictType" label="字典类型编码" rules={[{ required: true }]}>
                                <Input disabled={!!editingRecord} placeholder="如：sys_status" />
                            </Form.Item>
                            <Form.Item name="status" label="状态" initialValue={1}>
                                <Select options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
                            </Form.Item>
                            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
                        </>
                    ) : (
                        <>
                            <Form.Item name="dictType" label="字典类型" hidden><Input /></Form.Item>
                            <Form.Item name="dictLabel" label="字典标签" rules={[{ required: true }]}><Input placeholder="如：正常" /></Form.Item>
                            <Form.Item name="dictValue" label="字典键值" rules={[{ required: true }]}><Input placeholder="如：1" /></Form.Item>
                            <Form.Item name="listClass" label="回显样式" tooltip="用于表格 Tag 颜色">
                                <Select allowClear options={[
                                    { value: 'green', label: 'success(绿色)' },
                                    { value: 'red', label: 'danger(红色)' },       // ← danger 改为 red
                                    { value: 'orange', label: 'warning(橙色)' },
                                    { value: 'blue', label: 'primary(蓝色)' },
                                    { value: 'cyan', label: 'cyan(青色)' },
                                    { value: 'purple', label: 'purple(紫色)' },
                                    { value: 'default', label: 'default(灰色)' },
                                ]} />
                            </Form.Item>
                            <Form.Item name="sortOrder" label="显示排序" initialValue={0}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
                            <Form.Item name="status" label="状态" initialValue={1}>
                                <Select options={[{ value: 1, label: '正常' }, { value: 0, label: '停用' }]} />
                            </Form.Item>
                        </>
                    )}
                </Form>
            </Modal>
        </Row>
    );
}
