import axios from 'axios';
import { message } from 'antd';

// 一个"预配置"的 axios 实例：以后所有请求都用它，不用每次写 baseURL
const request = axios.create({
  baseURL: '/api',       // 配合 vite 的 proxy，实际打到 http://127.0.0.1:8080/api
  timeout: 10000,
});

// ---------- 请求拦截器：出门前贴 token ----------
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    // 后端 JwtAuthFilter 读的就是这个头，格式必须是 "Bearer 空格 token"
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ---------- 响应拦截器：回来时剥壳 + 统一报错 ----------
request.interceptors.response.use(
  // 走到这里说明 HTTP 状态码是 2xx
  (response) => {
    const body = response.data;           // 后端统一返回 { code, msg, data }
    if (body.code === 0) {
      return body.data;                   // 成功：只把 data 交给业务代码
    }
    message.error(body.msg || '请求失败');
    return Promise.reject(new Error(body.msg));
  },
  // 走到这里说明 HTTP 状态码是 4xx / 5xx（我们的 BizException 就是这种）
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;

    if (status === 401) {
      // token 过期 / 被踢下线（比如 redis 里 auth:user:1 被删了）
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        message.warning('登录已过期，请重新登录');
        window.location.href = '/login';
      }
    } else {
      // 400/403/409 这些，后端 msg 就是给人看的中文，直接弹
      message.error(body?.msg || `请求失败（${status || '网络错误'}）`);
    }
    return Promise.reject(error);
  },
);

export default request;