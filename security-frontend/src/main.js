import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'          // 引入 Element Plus
import 'element-plus/dist/index.css'            // 引入 Element Plus 的全局样式
import zhCn from 'element-plus/es/locale/lang/zh-cn' // 引入中文语言包
import axios from 'axios'                       // 引入 axios 库，用于发送 HTTP 请求

// 配置 axios 的基础 URL，确保请求能正确转发到后端服务
axios.defaults.baseURL = process.env.VUE_APP_API_URL || 'http://localhost:8081'

// 添加请求拦截器，自动为每个请求添加用户角色请求头
axios.interceptors.request.use(config => {
  const role = localStorage.getItem('userRole')
  if (role) {
    config.headers['X-User-Role'] = role
  }
  return config
})

// 初始化 Vue 应用
const app = createApp(App)

app.use(ElementPlus, { locale: zhCn })   // 注册 Element Plus 并设置中文
app.mount('#app')
