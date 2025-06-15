// src/utils/axiosInstance.js
import axios from 'axios'
import auth from './auth'

// 创建axios实例
const instance = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL, // 从环境变量获取基础URL
  timeout: 10000 // 请求超时时间
})
let isRedirecting = false
instance.interceptors.request.use(
  (config) => {
    // 检查token是否过期
    const token = auth.getToken()
    if (token) {
      // 添加认证头
      config.headers['Authorization'] = `${token}`

      // 检查过期
      if (auth.isTokenExpired() && !isRedirecting) {
        isRedirecting = true
        auth.clearToken()

        // 客户端跳转
        if (process.client) {
          console.warn('[Auth] Token expired, redirecting to login')
          window.location.href = '/login?expired=1'
        }

        return Promise.reject(new Error('Token expired'))
      }
    }
    return config
  },
  (error) => {
    console.error('[Axios Request Error]', error)
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    // 处理HTTP错误状态码 401
    // 身份验证凭证过期
    if (error.response) {
      switch (error.response.status) {
        case 401:
          window.location.href = '/login'
          break
      }
    }

    // 返回错误信息
    return Promise.reject(error)
  }
)

export default instance
