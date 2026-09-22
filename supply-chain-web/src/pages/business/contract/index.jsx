import { useState, useEffect } from 'react';
import { Card, Table, Button, Form, Input, Select, Tag, Modal, Descriptions, message, Space, DatePicker } from 'antd';
import { EyeOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { getContractPageApi, getContractDetailApi, auditContractApi, getCategoryListApi } from '../../../api/business';
import Auth from '../../../components/Auth';
import { getTenantListApi } from '../../../api/system'; // 引入

export default function ContractManagement() {
    const [contracts, setContracts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

    // 详情弹窗状态
    const [detailVisible, setDetailVisible] = useState(false);
    const [currentContract, setCurrentContract] = useState(null);
    const [detailFields, setDetailFields] = useState([]);

    const [searchForm] = Form.useForm();

    // 1. 初始化加载
    useEffect(() => {
        fetchCategories();
        fetchContracts();
        // 获取租户列表 (仅超管需要，普通租户获取了也不影响)
        getTenantListApi().then(res => setTenants(res || [])).catch(() => {});
    }, []);

    const fetchCategories = async () => {
        try {
            const res = await getCategoryListApi();
            setCategories(res || []);
        } catch (error) {}
    };

    // 2. 获取合同列表
    const fetchContracts = async (params = {}) => {
        setLoading(true);
        try {
            const res = await getContractPageApi({
                pageNum: pagination.current,
                pageSize: pagination.pageSize,
                ...params
            });
            setContracts(res?.records || []);
            setPagination(prev => ({ ...prev, total: res?.total || 0 }));
        } catch (error) {
            message.error('获取合同列表失败');
        } finally {
            setLoading(false);
        }
    };

    // 3. 搜索与重置
    const handleSearch = (values) => {
        setPagination(prev => ({ ...prev, current: 1 }));
        fetchContracts(values);
    };

    const handleReset = () => {
        searchForm.resetFields();
        handleSearch({});
    };

    // 4. 查看详情 (解析动态字段)
    const handleViewDetail = async (record) => {
        try {
            // 获取完整详情
            const detail = await getContractDetailApi(record.id);
            setCurrentContract(detail);

            // 获取该品类的字段配置，用于回显动态字段的 Label
            const fieldsRes = await getCategoryFieldsApi(detail.categoryId);
            setDetailFields(fieldsRes || []);

            setDetailVisible(true);
        } catch (error) {
            message.error('获取详情失败');
        }
    };

    // 5. 审核操作
    const handleAudit = async (id, status) => {
        const actionText = status === 1 ? '生效' : '作废';
        Modal.confirm({
            title: `确认${actionText}`,
            content: `确定要将该合同状态变更为【${status === 1 ? '已生效' : '已作废'}】吗？`,
            onOk: async () => {
                try {
                    await auditContractApi({ id, status });
                    message.success(`${actionText}成功`);
                    fetchContracts(searchForm.getFieldsValue());
                } catch (error) {
                    message.error('操作失败');
                }
            }
        });
    };

    // 6. 表格列定义
    const columns = [
        { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 180 },
        {
            title: '所属品类',
            dataIndex: 'categoryId',
            key: 'categoryId',
            render: (id) => categories.find(c => c.id === id)?.name || '-'
        },
        {
            title: '所属租户',
            dataIndex: 'tenantId',
            key: 'tenantId',
            width: 150,
            // 只有超管(tenantId=0)才显示这一列，普通租户隐藏
            render: (id) => {
                const tenant = tenants.find(t => t.id === id);
                return tenant ? tenant.name : '未知租户';
            }
        },
        {
            title: '合同金额',
            dataIndex: 'amount',
            key: 'amount',
            render: (val) => val ? `￥${Number(val).toFixed(2)}` : '-'
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status) => {
                const map = { 0: { text: '草稿', color: 'default' }, 1: { text: '已生效', color: 'success' }, 2: { text: '已作废', color: 'danger' } };
                const item = map[status] || { text: '未知', color: 'default' };
                return <Tag color={item.color}>{item.text}</Tag>;
            }
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 180,
            render: (t) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
        },
        {
            title: '操作',
            key: 'action',
            width: 200,
            fixed: 'right',
            render: (_, record) => (
                <Space>
                    <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>

                    <Auth perm="business:contract:audit">
                        {record.status === 0 && (
                            <>
                                <Button type="link" size="small" style={{ color: '#52c41a' }} icon={<CheckCircleOutlined />} onClick={() => handleAudit(record.id, 1)}>生效</Button>
                                <Button type="link" size="small" danger icon={<CloseCircleOutlined />} onClick={() => handleAudit(record.id, 2)}>作废</Button>
                            </>
                        )}
                    </Auth>
                </Space>
            )
        }
    ];

    return (
        <Card title="合同管理">
            {/* 搜索区域 */}
            <Form form={searchForm} layout="inline" onFinish={handleSearch} style={{ marginBottom: 16 }}>
                <Form.Item name="contractNo">
                    <Input placeholder="合同编号" allowClear />
                </Form.Item>
                <Form.Item name="categoryId">
                    <Select
                        placeholder="选择品类"
                        allowClear
                        style={{ width: 160 }}
                        options={categories.map(c => ({ label: c.name, value: c.id }))}
                    />
                </Form.Item>
                <Form.Item name="status">
                    <Select
                        placeholder="状态"
                        allowClear
                        style={{ width: 120 }}
                        options={[
                            { label: '草稿', value: 0 },
                            { label: '已生效', value: 1 },
                            { label: '已作废', value: 2 }
                        ]}
                    />
                </Form.Item>
                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit">搜索</Button>
                        <Button onClick={handleReset}>重置</Button>
                    </Space>
                </Form.Item>
            </Form>

            {/* 数据表格 */}
            <Table
                columns={columns}
                dataSource={contracts}
                rowKey="id"
                loading={loading}
                scroll={{ x: 1000 }}
                pagination={{
                    ...pagination,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条`,
                    onChange: (page, pageSize) => {
                        setPagination(prev => ({ ...prev, current: page, pageSize }));
                        fetchContracts({ ...searchForm.getFieldsValue(), pageNum: page, pageSize });
                    }
                }}
            />

            {/* 详情弹窗 (包含动态字段解析) */}
            <Modal
                title="合同详情"
                open={detailVisible}
                onCancel={() => setDetailVisible(false)}
                footer={<Button onClick={() => setDetailVisible(false)}>关闭</Button>}
                width={700}
            >
                {currentContract && (
                    <>
                        <Descriptions bordered column={2} size="small">
                            <Descriptions.Item label="合同编号">{currentContract.contractNo}</Descriptions.Item>
                            <Descriptions.Item label="所属品类">
                                {categories.find(c => c.id === currentContract.categoryId)?.name || '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="合同金额">￥{Number(currentContract.amount).toFixed(2)}</Descriptions.Item>
                            <Descriptions.Item label="当前状态">
                                {currentContract.status === 1 ? <Tag color="success">已生效</Tag> : currentContract.status === 2 ? <Tag color="danger">已作废</Tag> : <Tag>草稿</Tag>}
                            </Descriptions.Item>
                        </Descriptions>

                        {/* 动态字段展示 */}
                        {detailFields.length > 0 && currentContract.extraData && (
                            <>
                                <div style={{ margin: '16px 0 8px', fontWeight: 'bold', fontSize: 14 }}>品类专属信息</div>
                                <Descriptions bordered column={2} size="small">
                                    {detailFields.map(field => (
                                        <Descriptions.Item key={field.fieldKey} label={field.fieldLabel}>
                                            {currentContract.extraData[field.fieldKey] || '-'}
                                        </Descriptions.Item>
                                    ))}
                                </Descriptions>
                            </>
                        )}
                    </>
                )}
            </Modal>
        </Card>
    );
}
