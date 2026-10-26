import axios from 'axios'
import { API_BASE_URL } from '@/config'

/**
 * 全局 Axios 实例
 *
 * 说明：聊天接口是 SSE 流（text/event-stream），浏览器里 Axios 无法消费流式响应，
 * 所以真正的流式请求用 EventSource 发起，但地址依然复用这里的 baseURL，
 * 保证“接口前缀”只有一处配置。
 */
const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60_000,
  headers: {
    'Content-Type': 'application/json'
  }
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error?.response?.data?.message ||
      error?.response?.statusText ||
      error?.message ||
      '网络请求失败'
    return Promise.reject(new Error(message))
  }
)

export default http
