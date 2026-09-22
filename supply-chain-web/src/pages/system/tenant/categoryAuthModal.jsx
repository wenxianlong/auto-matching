import { useState, useEffect } from 'react';
import { Modal, Checkbox, Spin, message, Empty, Tag } from 'antd';
import { getTenantCategoriesApi, saveTenantCategoriesApi, getCategoryListApi } from '../../../api/business';

export default function CategoryAuthModal({ open, tenant, onCancel, onSuccess }) {
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [allCategories, setAllCategories] = useState([]);
    const [checkedIds, setCheckedIds] = useState([]);

    // 弹窗打开时加载数据
    useEffect(() => {
        if (open && tenant?.id) {
            fetchData(tenant.id);
        }
    }, [open, tenant]);

    const fetchData = async (tenantId) => {
        setLoading(true);
        try {
            // 并行请求：所有品类 + 该租户已授权的品类
            const [allRes, authRes] = await Promise.all([
                getCategoryListApi(),
                getTenantCategoriesApi(tenantId)
            ]);
            setAllCategories(allRes || []);
            setCheckedIds(authRes || []);
        } catch (error) {
            message.error('加载品类数据失败');
        } finally {
            setLoading(false);
        }
    };

    // 勾选/取消勾选
    const handleCheckChange = (categoryId, checked) => {
        setCheckedIds(prev =>
            checked ? [...prev, categoryId] : prev.filter(id => id !== categoryId)
        );
    };

    // 提交保存
    const handleOk = async () => {
        if (!tenant?.id) return;
        setSubmitting(true);
        try {
            await saveTenantCategoriesApi(tenant.id, checkedIds);
            message.success('品类授权保存成功');
            onSuccess?.();
            onCancel();
        } catch (error) {
            message.error('保存失败');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Modal
            title={`分配品类 - ${tenant?.name || ''}`}
            open={open}
            onCancel={onCancel}
            onOk={handleOk}
            confirmLoading={submitting}
            destroyOnHidden
            width={600}
        >
            <Spin spinning={loading}>
                {allCategories.length === 0 && !loading ? (
                    <Empty description="暂无可用品类，请先在品类管理中创建" />
                ) : (
                    <Checkbox.Group
                        value={checkedIds}
                        style={{ display: 'flex', flexDirection: 'column', gap: 12 }}
                    >
                        {allCategories.map(cat => (
                            <Checkbox
                                key={cat.id}
                                value={cat.id}
                                onChange={e => handleCheckChange(cat.id, e.target.checked)}
                                style={{ marginLeft: 0 }}
                            >
                                <span style={{ fontWeight: 500 }}>{cat.name}</span>
                                <Tag color="blue" style={{ marginLeft: 8 }}>{cat.code}</Tag>
                            </Checkbox>
                        ))}
                    </Checkbox.Group>
                )}
            </Spin>
        </Modal>
    );
}
