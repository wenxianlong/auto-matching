import { useState, useEffect } from 'react';
import { Card, Form, Input, InputNumber, Select, DatePicker, Button, message, Spin, Divider, Empty } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { getMyCategoriesApi, getCategoryFieldsApi, createContractApi } from '../../../api/business';
import DictSelect from '../../../components/DictSelect'; // 复用字典组件

export default function CreateContract() {
    const [form] = Form.useForm();
    const [categories, setCategories] = useState([]);
    const [fields, setFields] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    // 1. 初始化：获取当前租户可用的品类
    useEffect(() => {
        const fetchCategories = async () => {
            setLoading(true);
            try {
                const res = await getMyCategoriesApi();
                setCategories(res || []);
            } catch (error) {
                message.error('获取可用品类失败');
            } finally {
                setLoading(false);
            }
        };
        fetchCategories();
    }, []);

    // 2. 品类切换：动态加载字段配置
    const handleCategoryChange = async (categoryId) => {
        form.setFieldsValue({ extraData: {} }); // 清空之前的动态字段数据
        if (!categoryId) {
            setFields([]);
            return;
        }

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

    // 3. 提交合同
    const handleSubmit = async (values) => {
        setSubmitting(true);
        try {
            // 处理日期格式：将 dayjs 对象转为字符串
            const extraData = values.extraData || {};
            const formattedExtraData = {};

            fields.forEach(field => {
                let val = extraData[field.fieldKey];
                if (field.fieldType === 'date' && val) {
                    formattedExtraData[field.fieldKey] = val.format('YYYY-MM-DD');
                } else {
                    formattedExtraData[field.fieldKey] = val;
                }
            });

            const payload = {
                contractNo: values.contractNo,
                categoryId: values.categoryId,
                amount: values.amount,
                extraData: formattedExtraData
            };

            await createContractApi(payload);
            message.success('合同提交成功！');
            form.resetFields();
            setFields([]);
        } catch (error) {
            message.error(error.message || '提交失败');
        } finally {
            setSubmitting(false);
        }
    };

    // 4. 动态渲染表单控件
    const renderFieldControl = (field) => {
        const placeholder = `请${field.fieldType === 'select' || field.fieldType === 'date' ? '选择' : '输入'}${field.fieldLabel}`;

        switch (field.fieldType) {
            case 'textarea':
                return <Input.TextArea placeholder={placeholder} rows={3} />;
            case 'number':
                return <InputNumber style={{ width: '100%' }} placeholder={placeholder} />;
            case 'date':
                return <DatePicker style={{ width: '100%' }} placeholder={placeholder} />;
            case 'select':
                return <DictSelect dictType={field.dictType} placeholder={placeholder} />;
            case 'input':
            default:
                return <Input placeholder={placeholder} />;
        }
    };

    return (
        <div style={{ padding: 24, background: '#f5f5f5', minHeight: '100vh' }}>
            <Card
                title="创建业务合同"
                style={{ maxWidth: 800, margin: '0 auto' }}
                headStyle={{ fontSize: 18, fontWeight: 'bold' }}
            >
                <Spin spinning={loading}>
                    <Form
                        form={form}
                        layout="vertical"
                        onFinish={handleSubmit}
                        requiredMark="optional"
                    >
                        <Form.Item
                            name="contractNo"
                            label="合同编号"
                            rules={[{ required: true, message: '请输入合同编号' }]}
                        >
                            <Input placeholder="如：HT-20231024-001" size="large" />
                        </Form.Item>

                        <Form.Item
                            name="categoryId"
                            label="选择业务品类"
                            rules={[{ required: true, message: '请选择品类' }]}
                        >
                            <Select
                                placeholder="请选择合同所属品类"
                                options={categories.map(c => ({ label: c.name, value: c.id }))}
                                onChange={handleCategoryChange}
                                size="large"
                                allowClear
                            />
                        </Form.Item>

                        <Form.Item
                            name="amount"
                            label="合同金额 (元)"
                            rules={[{ required: true, message: '请输入金额' }]}
                        >
                            <InputNumber
                                prefix="￥"
                                style={{ width: '100%' }}
                                placeholder="请输入合同总金额"
                                size="large"
                                min={0}
                                precision={2}
                            />
                        </Form.Item>

                        {/* 动态字段区域 */}
                        {fields.length > 0 && (
                            <>
                                <Divider orientation="left" style={{ margin: '24px 0 16px' }}>
                                    品类专属信息
                                </Divider>
                                <div style={{ background: '#fafafa', padding: 16, borderRadius: 8, marginBottom: 24 }}>
                                    {fields.map(field => (
                                        <Form.Item
                                            key={field.fieldKey}
                                            name={['extraData', field.fieldKey]} // 嵌套字段，自动存入 extraData 对象
                                            label={field.fieldLabel}
                                            rules={[{
                                                required: field.isRequired === 1,
                                                message: `${field.fieldLabel}不能为空`
                                            }]}
                                            style={{ marginBottom: 16 }}
                                        >
                                            {renderFieldControl(field)}
                                        </Form.Item>
                                    ))}
                                </div>
                            </>
                        )}

                        {fields.length === 0 && !loading && (
                            <Empty description="请先选择品类以加载专属字段" style={{ margin: '40px 0' }}/>
                        )}

                        <Form.Item style={{ marginTop: 32 }}>
                            <Button
                                type="primary"
                                htmlType="submit"
                                block
                                size="large"
                                icon={<SendOutlined />}
                                loading={submitting}
                                disabled={fields.length === 0}
                            >
                                提交合同
                            </Button>
                        </Form.Item>
                    </Form>
                </Spin>
            </Card>
        </div>
    );
}
