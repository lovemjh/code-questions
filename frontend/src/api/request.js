import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一的 axios 实例：基础路径 /api（开发环境由 Vite 代理转发到后端）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    // 后端统一 Result 结构：{code, message, data}，成功时直接返回 data
    if (result && typeof result === 'object' && 'code' in result) {
      if (result.code === 200) {
        return result.data
      }
      ElMessage.error(result.message || '操作失败')
      return Promise.reject(new Error(result.message || '操作失败'))
    }
    return result
  },
  (error) => {
    // HTTP 非 2xx（后端仍返回 Result 结构）或网络异常
    const result = error.response && error.response.data
    const message = (result && result.message) || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
