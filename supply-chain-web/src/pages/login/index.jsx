import { Form, Input, Button, Card, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './Login.css';

const { Title } = Typography;

export default function Login() {
    const navigate = useNavigate();
    const { login, loading } = useAuth();

    const onFinish = async (values) => {
        try {
            await login(values.username, values.password);
            message.success('登录成功');
            navigate('/');
        } catch (error) {
            message.error('用户名或密码错误');
        }
    };

    return (
        <div className="login-container">
            <Card className="login-card" bordered={false}>
                <div className="login-header">
                    <Title level={3}>大宗商品供应链系统</Title>
                    <p>多租户 · 多品类 · 动态配置</p>
                </div>
                <Form name="login" onFinish={onFinish} size="large">
                    <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                        <Input prefix={<UserOutlined />} placeholder="用户名 (admin)" />
                    </Form.Item>
                    <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="密码 (123456)" />
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            登 录
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </div>
    );
}
