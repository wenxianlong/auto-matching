import { useState } from 'react';
import {
    Card, Form, InputNumber, Select, Button, Table, Tag, message,
    Descriptions, Empty, Spin, Divider, Row, Col, Space
} from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';
import request from '../../../api/request';

export default function GroupingManagement() {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [results, setResults] = useState([]);

    const handleCalculate = async (values) => {
        setLoading(true);
        try {
            // 过滤掉值为 null 或 undefined 的字段，保持请求体干净
            const cleanValues = Object.fromEntries(
                Object.entries(values).filter(([_, v]) => v != null && v !== '')
            );

            const res = await request.post('/admin/business/grouping/calculate', cleanValues);
            setResults(res || []);
            if (!res || res.length === 0) {
                message.warning('未找到满足条件的配组方案，请调整参数范围或增加库存');
            } else {
                message.success(`计算完成，为您找到 ${res.length} 个推荐方案`);
            }
        } catch (error) {
            message.error(error?.response?.data?.msg || '计算失败，请检查参数');
        } finally {
            setLoading(false);
        }
    };

    const modeOptions = [
        { value: 'LEAST_STOCK', label: '优先使用最少库存 (清尾数)' },
        { value: 'MOST_STOCK', label: '优先使用最多库存 (减批次)' },
        { value: 'FIT_VOLTAGE', label: '优先满足电压 (大电压优先)' },
        { value: 'FIT_HEIGHT', label: '优先满足高度 (大高度优先)' }
    ];

    const columns = [
        { title: '编码', dataIndex: 'code', key: 'code', width: 120, fixed: 'left' },
        { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 100 },
        { title: '型号', dataIndex: 'model', key: 'model', width: 100 },
        {
            title: '使用片数', // 【新增修复】：明确展示该型号/批次在本次配组中使用的片数
            dataIndex: 'usedCount',
            key: 'usedCount',
            width: 90,
            align: 'center',
            fixed: 'left', // 固定在左侧，方便和编码、批次号一起查看
            render: (count) => <Tag color="volcano" style={{ fontSize: 14, fontWeight: 'bold' }}>{count} 片</Tag>
        },
        {
            title: '当前库存', // 【新增修复】：展示该批次/型号的当前总库存
            dataIndex: 'currentStock',
            key: 'currentStock',
            width: 90,
            align: 'center',
            fixed: 'left',
            render: (stock, record) => {
                // 如果库存刚好等于使用片数（说明这批货要被用光了），标红预警
                const isRunningOut = stock === record.usedCount;
                return (
                    <span style={{
                        color: isRunningOut ? '#ff4d4f' : '#52c41a',
                        fontWeight: isRunningOut ? 'bold' : 'normal'
                    }}>
            {stock} 片
          </span>
                );
            }
        },
        { title: '高度(mm)', dataIndex: 'height', key: 'height', width: 90, align: 'right' },
        { title: '电压(kV)', dataIndex: 'voltage', key: 'voltage', width: 90, align: 'right' },
        { title: '高方波', dataIndex: 'highSquareWave', key: 'highSquareWave', width: 90, align: 'right' },
        { title: '大电流', dataIndex: 'largeCurrent', key: 'largeCurrent', width: 90, align: 'right' },
        { title: '漏流', dataIndex: 'leakageCurrent', key: 'leakageCurrent', width: 90, align: 'right' },
        { title: '残压', dataIndex: 'residualVoltage', key: 'residualVoltage', width: 90, align: 'right' },
        { title: '重转', dataIndex: 'heavyTransfer', key: 'heavyTransfer', width: 90, align: 'right' },
        { title: '全电流', dataIndex: 'totalCurrent', key: 'totalCurrent', width: 90, align: 'right' },
    ];

    return (
        <Card title="电阻片自动配组计算" bordered={false}>
            <Form
                form={form}
                layout="vertical"
                onFinish={handleCalculate}
                initialValues={{
                    targetDiameter: 48,
                    minTargetHeight: 123, maxTargetHeight: 125,
                    minTargetVoltage: 26.5, maxTargetVoltage: 33.0,
                    mode: 'FIT_HEIGHT'
                }}
            >
                {/* ================= 1. 基础参数区 ================= */}
                <Row gutter={24}>
                    <Col span={6}>
                        <Form.Item name="targetDiameter" label="目标直径(mm)" rules={[{ required: true, message: '请输入' }]}>
                            <InputNumber min={1} style={{ width: '100%' }} placeholder="如: 48" />
                        </Form.Item>
                    </Col>

                    <Col span={9}>
                        <Form.Item label="目标高度(mm)" required>
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minTargetHeight" noStyle rules={[{ required: true, message: '最小' }]}>
                                    <InputNumber placeholder="最小" style={{ width: '45%' }} />
                                </Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxTargetHeight" noStyle rules={[{ required: true, message: '最大' }]}>
                                    <InputNumber placeholder="最大" style={{ width: '45%' }} />
                                </Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>

                    <Col span={9}>
                        <Form.Item label="目标电压(kV)" required>
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minTargetVoltage" noStyle rules={[{ required: true, message: '最小' }]}>
                                    <InputNumber placeholder="最小" step={0.1} style={{ width: '45%' }} />
                                </Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxTargetVoltage" noStyle rules={[{ required: true, message: '最大' }]}>
                                    <InputNumber placeholder="最大" step={0.1} style={{ width: '45%' }} />
                                </Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                </Row>

                {/* ================= 2. 测试参数范围约束区 ================= */}
                <Divider orientation="left" style={{ margin: '8px 0 16px', fontSize: 14 }}>
                    测试参数范围约束 (选填，用于精准筛选)
                </Divider>

                <Row gutter={24}>
                    <Col span={8}>
                        <Form.Item label="高方波值">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minHighSquareWave" noStyle><InputNumber placeholder="最小" step={0.01} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxHighSquareWave" noStyle><InputNumber placeholder="最大" step={0.01} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                    <Col span={8}>
                        <Form.Item label="大电流">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minLargeCurrent" noStyle><InputNumber placeholder="最小" step={0.01} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxLargeCurrent" noStyle><InputNumber placeholder="最大" step={0.01} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                    <Col span={8}>
                        <Form.Item label="漏流">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minLeakageCurrent" noStyle><InputNumber placeholder="最小" step={0.001} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxLeakageCurrent" noStyle><InputNumber placeholder="最大" step={0.001} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>

                    <Col span={8}>
                        <Form.Item label="残压">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minResidualVoltage" noStyle><InputNumber placeholder="最小" step={0.01} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxResidualVoltage" noStyle><InputNumber placeholder="最大" step={0.01} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                    <Col span={8}>
                        <Form.Item label="重转">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minHeavyTransfer" noStyle><InputNumber placeholder="最小" step={0.01} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxHeavyTransfer" noStyle><InputNumber placeholder="最大" step={0.01} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                    <Col span={8}>
                        <Form.Item label="全电流">
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item name="minTotalCurrent" noStyle><InputNumber placeholder="最小" step={0.01} style={{ width: '45%' }} /></Form.Item>
                                <InputNumber placeholder="~" disabled style={{ width: '10%', pointerEvents: 'none' }} />
                                <Form.Item name="maxTotalCurrent" noStyle><InputNumber placeholder="最大" step={0.01} style={{ width: '45%' }} /></Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Col>
                </Row>

                {/* ================= 3. 操作区 ================= */}
                <Row gutter={24} align="middle" style={{ marginTop: 8 }}>
                    <Col span={8}>
                        <Form.Item name="mode" label="配组模式" rules={[{ required: true }]}>
                            <Select options={modeOptions} placeholder="请选择模式" />
                        </Form.Item>
                    </Col>
                    <Col span={16} style={{ textAlign: 'right' }}>
                        <Form.Item>
                            <Button
                                type="primary"
                                htmlType="submit"
                                icon={<ThunderboltOutlined />}
                                loading={loading}
                                size="large"
                            >
                                开始自动配组
                            </Button>
                        </Form.Item>
                    </Col>
                </Row>
            </Form>

            {/* ================= 4. 结果展示区 ================= */}
            <Spin spinning={loading} tip="正在计算最优配组方案，请稍候...">
                {results.length > 0 ? (
                    results.map((result, index) => (
                        <Card
                            key={index}
                            title={
                                <span>
                  方案 {index + 1}
                                    {index === 0 && <Tag color="gold" style={{ marginLeft: 8 }}>最优推荐</Tag>}
                </span>
                            }
                            style={{ marginTop: 16 }}
                            bordered
                        >
                            <Descriptions column={4} bordered size="small" style={{ marginBottom: 16 }}>
                                <Descriptions.Item label="可配置组数">
                                    <Tag color="green" style={{ fontSize: 16, padding: '2px 10px' }}>
                                        {result.maxProducibleGroups} 组
                                    </Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="总使用片数">
                                    <Tag color="blue">{result.totalPieces} 片/组</Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="单组总高度">{result.totalHeight} mm</Descriptions.Item>
                                <Descriptions.Item label="单组总电压">{result.totalVoltage} kV</Descriptions.Item>
                            </Descriptions>
                            <Table
                                columns={columns}
                                dataSource={result.details}
                                rowKey="resistorId"
                                pagination={false}
                                size="small"
                                scroll={{ x: 1200 }}
                                bordered
                            />
                        </Card>
                    ))
                ) : (
                    !loading && <Empty description="请输入参数并点击计算，这里将展示推荐的配组方案" style={{ marginTop: 40 }} />
                )}
            </Spin>
        </Card>
    );
}
