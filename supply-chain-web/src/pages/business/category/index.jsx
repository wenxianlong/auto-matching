import { useState, useEffect } from 'react';
import { Row, Col, Card, Table, Button, Form, Input, Select, Switch, message, Space, Popconfirm, Modal, Spin } from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined, EditOutlined } from '@ant-design/icons';
import Auth from '../../../components/Auth'; // 引入权限控制组件
import {
    getCategoryListApi,
    addCategoryApi,
    updateCategoryApi,
    deleteCategoryApi,
    getCategoryFieldsApi,
    saveCategoryFieldsApi
} from '../../../api/business'; // 引入封装好的 API

export default function CategoryManagement() {
    const [categories, setCategories] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [fields, setFields] = useState([]);
    const [loading, setLoading] = useState(false);
    const [savingFields, setSavingFields] = useState(false);

    // 品类弹窗状态
    const [modalVisible, setModalVisible] = useState(false);
    const [editingCategory, setEditingCategory] = useState(null);
    const [submitLoading, setSubmitLoading] = useState(false);
    const [form] = Form.useForm();

    // ================= 1. 数据获取 =================
    const fetchCategories = async () => {
        setLoading(true);
        try {
            const res = await getCategoryListApi();
            setCategories(res || []);
        } catch (error) {
            message.error('获取品类列表失败');
        } finally {
            setLoading(false);
        }
    };

    const fetchFields = async (categoryId) => {
        setLoading(true);
        try {
            const res = await getCategoryFieldsApi(categoryId);
            setFields(res || []);
        } catch (error) {
            message.error('获取字段配置失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

    // ================= 2. 品类 CRUD 操作 =================
    const handleSelectCategory = (record) => {
        setSelectedCategory(record);
        fetchFields(record.id);
    };

    const openCategoryModal = (record = null) => {
        setEditingCategory(record);
        form.resetFields();
        if (record) {
            form.setFieldsValue(record);
        }
        setModalVisible(true);
    };

    const handleSaveCategory = async (values) => {
        setSubmitLoading(true);
        try {
            if (editingCategory) {
                await updateCategoryApi({ ...editingCategory, ...values });
                message.success('修改成功');
            } else {
                await addCategoryApi(values);
                message.success('新增成功');
            }
            setModalVisible(false);
            fetchCategories();
        } catch (error) {
            message.error('保存失败');
        } finally {
            setSubmitLoading(false);
        }
    };

    const handleDeleteCategory = async (id) => {
        try {
            await deleteCategoryApi(id);
            message.success('删除成功');
            // 如果删除的是当前选中的品类，清空右侧
            if (selectedCategory && selectedCategory.id === id) {
                setSelectedCategory(null);
                setFields([]);
            }
            fetchCategories();
        } catch (error) {
            message.error('删除失败');
        }
    };

    // ================= 3. 字段配置操作 =================
    const handleAddField = () => {
        setFields([
            ...fields,
            {
                fieldKey: '',
                fieldLabel: '',
                fieldType: 'input',
                isRequired: 0,
                dictType: '',
                sortOrder: fields.length + 1
            }
        ]);
    };

    const handleFieldChange = (index, key, value) => {
        const newFields = [...fields];
        newFields[index][key] = value;
        setFields(newFields);
    };

    const handleRemoveField = (index) => {
        const newFields = fields.filter((_, i) => i !== index);
        setFields(newFields);
    };

    const handleSaveFields = async () => {
        if (!selectedCategory) return;

        // 前端基础校验
        for (let i = 0; i < fields.length; i++) {
            const f = fields[i];
            if (!f.fieldKey || !f.fieldLabel) {
                message.error(`第 ${i + 1} 行的字段标识和名称不能为空`);
                return;
            }
            // 检查 fieldKey 是否重复
            const keys = fields.map(item => item.fieldKey);
            if (keys.indexOf(f.fieldKey) !== keys.lastIndexOf(f.fieldKey)) {
                message.error(`字段标识 "${f.fieldKey}" 重复，请修改`);
                return;
            }
        }

        setSavingFields(true);
        try {
            await saveCategoryFieldsApi(selectedCategory.id, fields);
            message.success('字段配置保存成功');
            fetchFields(selectedCategory.id);
        } catch (error) {
            message.error('保存失败');
        } finally {
            setSavingFields(false);
        }
    };

    const fieldTypeOptions = [
        { value: 'input', label: '单行文本' },
        { value: 'textarea', label: '多行文本' },
        { value: 'number', label: '数字' },
        { value: 'select', label: '下拉选择' },
        { value: 'date', label: '日期' }
    ];

    // ================= 4. 表格列定义 =================
    const categoryColumns = [
        { title: '品类名称', dataIndex: 'name', key: 'name' },
        { title: '品类编码', dataIndex: 'code', key: 'code' },
        {
            title: '操作',
            key: 'action',
            width: 120,
            render: (_, record) => (
                <Space>
                    <Auth perm="business:category:edit">
                        <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={(e) => { e.stopPropagation(); openCategoryModal(record); }}
                        />
                    </Auth>
                    <Auth perm="business:category:remove">
                        <Popconfirm
                            title="确定删除该品类吗？"
                            onConfirm={(e) => { e.stopPropagation(); handleDeleteCategory(record.id); }}
                            onCancel={(e) => e.stopPropagation()}
                        >
                            <Button
                                type="link"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()}
                            />
                        </Popconfirm>
                    </Auth>
                </Space>
            )
        }
    ];

    const fieldColumns = [
        {
            title: '字段标识(英文)',
            dataIndex: 'fieldKey',
            width: 180,
            render: (v, _, i) => <Input value={v} onChange={e => handleFieldChange(i, 'fieldKey', e.target.value)} placeholder="如: material_code" />
        },
        {
            title: '字段名称',
            dataIndex: 'fieldLabel',
            width: 150,
            render: (v, _, i) => <Input value={v} onChange={e => handleFieldChange(i, 'fieldLabel', e.target.value)} placeholder="如: 物料编码" />
        },
        {
            title: '控件类型',
            dataIndex: 'fieldType',
            width: 140,
            render: (v, _, i) => <Select value={v} options={fieldTypeOptions} onChange={val => handleFieldChange(i, 'fieldType', val)} style={{ width: '100%' }} />
        },
        {
            title: '关联字典(可选)',
            dataIndex: 'dictType',
            width: 160,
            render: (v, _, i) => (
                <Input
                    value={v}
                    onChange={e => handleFieldChange(i, 'dictType', e.target.value)}
                    placeholder="如: sys_status"
                    disabled={fields[i]?.fieldType !== 'select'}
                />
            )
        },
        {
            title: '必填',
            dataIndex: 'isRequired',
            width: 70,
            align: 'center',
            render: (v, _, i) => <Switch checked={v === 1} onChange={val => handleFieldChange(i, 'isRequired', val ? 1 : 0)} />
        },
        {
            title: '操作',
            width: 60,
            align: 'center',
            render: (_, __, i) => (
                <Popconfirm title="确定删除该字段?" onConfirm={() => handleRemoveField(i)}>
                    <Button type="link" danger icon={<DeleteOutlined />} />
                </Popconfirm>
            )
        }
    ];

    // ================= 5. 渲染 UI =================
    return (
        <Row gutter={16}>
            {/* 左侧：品类管理 */}
            <Col span={8}>
                <Card
                    title="品类管理"
                    extra={
                        <Auth perm="business:category:add">
                            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCategoryModal()}>新增品类</Button>
                        </Auth>
                    }
                >
                    <Table
                        dataSource={categories}
                        rowKey="id"
                        pagination={false}
                        size="small"
                        loading={loading}
                        onRow={(record) => ({
                            onClick: () => handleSelectCategory(record),
                            style: {
                                cursor: 'pointer',
                                background: selectedCategory?.id === record.id ? '#e6f7ff' : 'transparent'
                            }
                        })}
                        columns={categoryColumns}
                    />
                </Card>
            </Col>

            {/* 右侧：字段配置 */}
            <Col span={16}>
                <Card
                    title={`字段配置 ${selectedCategory ? `[${selectedCategory.name}]` : ''}`}
                    extra={
                        <Space>
                            <Auth perm="business:category:configField">
                                <Button icon={<PlusOutlined />} onClick={handleAddField} disabled={!selectedCategory}>添加字段</Button>
                            </Auth>
                            <Auth perm="business:category:configField">
                                <Button
                                    type="primary"
                                    icon={<SaveOutlined />}
                                    onClick={handleSaveFields}
                                    loading={savingFields}
                                    disabled={!selectedCategory || fields.length === 0}
                                >
                                    保存配置
                                </Button>
                            </Auth>
                        </Space>
                    }
                >
                    <Spin spinning={loading}>
                        <Table
                            dataSource={fields}
                            rowKey={(_, index) => index}
                            pagination={false}
                            size="small"
                            columns={fieldColumns}
                            locale={{ emptyText: selectedCategory ? '暂无字段配置，请点击“添加字段”' : '请在左侧选择一个品类' }}
                        />
                    </Spin>
                </Card>
            </Col>

            {/* 新增/编辑品类弹窗 */}
            <Modal
                title={editingCategory ? '编辑品类' : '新增品类'}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => form.submit()}
                confirmLoading={submitLoading}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" onFinish={handleSaveCategory}>
                    <Form.Item name="name" label="品类名称" rules={[{ required: true, message: '请输入品类名称' }]}>
                        <Input placeholder="如：电子元器件" />
                    </Form.Item>
                    <Form.Item name="code" label="品类编码" rules={[{ required: true, message: '请输入品类编码' }]}>
                        <Input placeholder="如：ELEC" disabled={!!editingCategory} />
                    </Form.Item>
                </Form>
            </Modal>
        </Row>
    );
}
