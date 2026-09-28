import axios from 'axios'

// 统一的 axios 实例：基础路径 /api（开发环境由 Vite 代理转发到后端）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('请求异常:', error.message)
    return Promise.reject(error)
  }
)

export default request
