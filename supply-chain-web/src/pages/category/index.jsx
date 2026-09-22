import { useState, useEffect } from 'react';
import { Table, Button, Space, Card, Input, message, Tag } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { getCategoryPageApi } from '../../api/category';

export default function Category() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
    const [searchName, setSearchName] = useState('');

    const columns = [
        { title: '品类ID', dataIndex: 'id', key: 'id', width: 180 },
        { title: '品类名称', dataIndex: 'name', key: 'name' },
        { title: '品类编码', dataIndex: 'code', key: 'code', render: (text) => <Tag color="blue">{text}</Tag> },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
        {
            title: '操作', key: 'action',
            render: (_, record) => (
                <Space size="middle">
                    <a onClick={() => message.info(`配置 ${record.name} 的动态字段`)}>字段配置</a>
                    <a>编辑</a>
                </Space>
            ),
        },
    ];

    const fetchData = async (params = {}) => {
        setLoading(true);
        try {
            const res = await getCategoryPageApi({
                pageNum: params.current || pagination.current,
                pageSize: params.pageSize || pagination.pageSize,
                name: searchName,
            });
            setData(res.records);
            setPagination({ ...pagination, current: res.current, pageSize: res.size, total: res.total });
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, []);

    const handleTableChange = (pag) => { fetchData(pag); };

    return (
        <Card title="品类管理" extra={
            <Space>
                <Input placeholder="搜索品类名称" value={searchName} onChange={e => setSearchName(e.target.value)} style={{ width: 200 }} />
                <Button type="primary" icon={<SearchOutlined />} onClick={() => fetchData({ current: 1 })}>搜索</Button>
                <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>刷新</Button>
            </Space>
        }>
            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
                pagination={pagination}
                onChange={handleTableChange}
            />
        </Card>
    );
}
