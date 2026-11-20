import axios from 'axios'
import { ElMessage } from 'element-plus'

// 预配置的 axios 实例：所有请求都走它，baseURL 配合 vite proxy 打到后端
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：出门前贴 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    // 后端 JwtAuthFilter 读的就是这个头，格式必须是 "Bearer 空格 token"
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：回来时剥壳 + 统一报错
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code === 0) {
      return body.data
    }
    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new Error(body.msg))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        ElMessage.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(body?.msg || `请求失败（${status || '网络错误'}）`)
    }
    return Promise.reject(error)
  },
)

export default request
