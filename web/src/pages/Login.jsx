import { useState } from 'react';
import { Button, Card, Form, Input, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import request from '../api/request.js';
import { useUserStore } from '../store/user.js';

export default function Login() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();                          // 用来改地址
  const setLogin = useUserStore((s) => s.setLogin);

  // async/await：等后端回话期间代码停在这一行，回来再往下走
  async function handleFinish(values) {
    setLoading(true);
    try {
      // POST /api/auth/login，body 是 { username, password }
      const data = await request.post('/auth/login', values);
      // data = { token, tokenType, expiresIn, user: { id, username, nickname, roleCode } }
      setLogin(data.token, data.user);
      message.success(`欢迎，${data.user.nickname || data.user.username}`);
      navigate('/seats', { replace: true });   // replace: 按返回键不会回到登录页
    } catch {
      // 错误提示拦截器已经统一弹过了，这里故意什么都不写
    } finally {
      // 不管成功失败，按钮都要恢复可点
      setLoading(false);
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 80 }}>
      <Card title="图书馆座位预约 · 登录" style={{ width: 360 }}>
        <Form layout="vertical" onFinish={handleFinish}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="admin / zhangsan" size="large" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="123456" size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block size="large" loading={loading}>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}