import axios from 'axios'
import { ElMessage } from 'element-plus'

// 1. 创建 axios 实例
const service = axios.create({
  // 后端接口的基础地址 (根据你的后端配置，端口是 8080)
  baseURL: 'http://localhost:8080',
  // 请求超时时间 (5秒)
  timeout: 5000
})

// 2. 请求拦截器 (Request Interceptor)
// 在发送请求之前做些什么，比如把 Token 带给后端
service.interceptors.request.use(
  config => {
    // 假设登录后我们把 token 存在 localStorage 里
    const token = localStorage.getItem('token')
    if (token) {
      // 这里的 header 名字要和后端约定好，通常是 'Authorization' 或 'token'
      // 你的后端目前虽然是模拟 token，但这是一个好习惯
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 3. 响应拦截器 (Response Interceptor)
// 在收到后端响应后做些什么，比如统一处理错误
service.interceptors.response.use(
  response => {
    const res = response.data
    // 这里可以根据后端的 code 来判断是否请求成功
    // 比如你之前的 AuthController 返回 code: 200 表示成功
    return res
  },
  error => {
    console.error('请求错误:', error) // for debug
    ElMessage.error(error.message || '网络请求失败')
    return Promise.reject(error)
  }
)

export default service
