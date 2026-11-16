import { useEffect } from 'react';
import { Button, Layout, Menu, Space, Tag } from 'antd';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import Login from './pages/Login.jsx';
import SeatList from './pages/SeatList.jsx';
import MyBookings from './pages/MyBookings.jsx';
import request from './api/request.js';
import { useUserStore } from './store/user.js';

const { Header, Content } = Layout;

const menuItems = [
  { key: '/seats', label: <Link to="/seats">座位预约</Link> },
  { key: '/my', label: <Link to="/my">我的预约</Link> },
];

/** 路由守卫：没 token 就踢回登录页，并记住"本来想去哪" */
function RequireLogin({ children }) {
  const token = useUserStore((s) => s.token);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const token = useUserStore((s) => s.token);
  const user = useUserStore((s) => s.user);
  const setUser = useUserStore((s) => s.setUser);
  const logout = useUserStore((s) => s.logout);

  // 页面一打开就验证一次 token 还有没有效（可能已经被拉黑/踢下线）
  useEffect(() => {
    if (!token) return;
    request
      .get('/auth/me')
      .then((me) => setUser(me))
      .catch(() => logout());   // 401 的提示和跳转由拦截器处理
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
        <div style={{ color: '#fff', fontSize: 18, fontWeight: 600, whiteSpace: 'nowrap' }}>
          图书馆座位预约
        </div>

        {token && (
          <Menu
            theme="dark"
            mode="horizontal"
            selectedKeys={[location.pathname]}
            items={menuItems}
            style={{ flex: 1, minWidth: 0 }}
          />
        )}

        <Space style={{ marginLeft: 'auto' }}>
          {user && (
            <Tag color={user.roleCode === 'ADMIN' ? 'gold' : 'blue'}>
              {user.nickname || user.username}
            </Tag>
          )}
          {token ? (
            <Button size="small" onClick={handleLogout}>退出</Button>
          ) : (
            // 已经在登录页时不再重复给「去登录」入口
            location.pathname !== '/login' && (
              <Link to="/login" style={{ color: '#fff' }}>去登录</Link>
            )
          )}
        </Space>
      </Header>

      <Content style={{ padding: 24 }}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/seats"
            element={<RequireLogin><SeatList /></RequireLogin>}
          />
          <Route
            path="/my"
            element={<RequireLogin><MyBookings /></RequireLogin>}
          />
          {/* 其它任何地址：登录了就去座位页，没登录去登录页 */}
          <Route path="*" element={<Navigate to={token ? '/seats' : '/login'} replace />} />
        </Routes>
      </Content>
    </Layout>
  );
}