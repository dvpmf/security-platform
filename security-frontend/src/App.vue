<template>
  <!-- 应用根容器 -->
  <div id="app">
    <!-- 登录页面区域 -->
    <div v-if="!isLoggedIn" class="login-container">
      <!-- 登录表单卡片 -->
      <el-card class="login-card">
        <h2>工业园区智慧安防与应急联动系统</h2>
        <!-- 用户名输入框 双向绑定 -->
        <el-input v-model="username" placeholder="用户名" />
        <!-- 密码输入框 隐藏输入内容 -->
        <el-input 
          v-model="password" 
          type="password" 
          placeholder="密码" 
          style="margin-top:10px"
          @keyup.enter="login" 
        />
        <!-- 登录按钮 执行登录方法 -->
        <el-button type="primary" style="margin-top:10px; width:100%" @click="login">登录</el-button>
        <!-- 登录错误提示信息 -->
        <p v-if="loginMsg" style="color:red">{{ loginMsg }}</p>
      </el-card>
    </div>

    <!-- 主界面区域 -->
    <div v-else class="main-layout">
      <!-- 左侧导航栏 -->
      <div class="sidebar">
        <!-- 应用标题区域 -->
        <div class="logo-area">
          <span class="app-name">工业园区智慧安防与应急联动</span>
        </div>

        <!-- 主菜单导航 -->
        <el-menu
          :default-active="activeMenu"
          class="menu"
          background-color="#1e2a3a"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          @select="handleMenuSelect"
        >
          <!-- 设备管理菜单项 所有用户可见 -->
          <el-menu-item index="devices">
            <el-icon><Monitor /></el-icon>
            <span>设备管理</span>
          </el-menu-item>
          <!-- 传感器数据监控菜单项 所有用户可见 -->
          <el-menu-item index="sensor">
            <el-icon><DataLine /></el-icon>
            <span>传感器数据监控</span>
          </el-menu-item>
          <!-- 实时监控菜单项 所有用户可见 -->
          <el-menu-item index="camera">
            <el-icon><VideoCamera /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
          <!-- 告警记录菜单项 所有用户可见 -->
          <el-menu-item index="alerts">
            <el-icon><Warning /></el-icon>
            <span>告警记录</span>
          </el-menu-item>
          <!-- 应急演练菜单项 仅管理员可见 -->
          <el-menu-item v-if="userRole === 'admin'" index="drill">
            <el-icon><VideoPlay /></el-icon>
            <span>应急演练</span>
          </el-menu-item>
          <!-- 统计分析菜单项 仅管理员可见 -->
          <el-menu-item v-if="userRole === 'admin'" index="statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计分析</span>
          </el-menu-item>
          <!-- 应急预案菜单项 仅管理员可见 -->
          <el-menu-item v-if="userRole === 'admin'" index="emergency">
            <el-icon><Document /></el-icon>
            <span>应急预案</span>
          </el-menu-item>
        </el-menu>

        <!-- 用户信息区域 -->
        <div class="user-info">
          <!-- 用户头像 动态显示 -->
          <el-avatar :size="36" :src="userAvatar" />
          <!-- 用户详细信息 -->
          <div class="user-details">
            <span class="username">{{ userInfo?.username }}</span>
            <span class="role">{{ userRole === 'admin' ? '管理员' : '普通用户' }}</span>
          </div>
          <!-- 退出登录按钮 -->
          <el-button link @click="logout">退出</el-button>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="content">
        <div class="page-container">
          <!-- 动态组件渲染 根据菜单选择 -->
          <component :is="currentComponent" :userRole="userRole" />
        </div>
      </div>
    </div>

    <!-- 应急指挥面板 -->
    <el-dialog v-model="emergencyPanelVisible" title="应急指挥面板" width="500px" append-to-body>
      <div v-if="emergencyAlertData">
        <!-- 管理员显示完整信息 -->
        <template v-if="userRole === 'admin'">
          <h3 style="color: #f56c6c; margin-bottom: 16px;">⚠️ 演练告警</h3>
          <p><strong>告警类型：</strong>{{ emergencyAlertData.alertType || '未知类型' }}</p>
          <p><strong>告警内容：</strong>{{ emergencyAlertData.content || '无具体内容' }}</p>
          <p><strong>发生时间：</strong>{{ emergencyAlertData.createTime || '未知时间' }}</p>
          <p v-if="emergencyAlertData.planTitle" style="margin-top: 12px;">
            <strong>关联预案：</strong>{{ emergencyAlertData.planTitle }}
          </p>
          <p v-if="emergencyAlertData.planSteps" style="margin-top: 8px; white-space: pre-line;">
            <strong>处理步骤：</strong>{{ emergencyAlertData.planSteps }}
          </p>
        </template>
        <!-- 普通用户只显示简化提示 -->
        <template v-else>
          <h3 style="color: #f56c6c; margin-bottom: 16px; text-align: center;">⚠️ 紧急警报</h3>
          <p style="text-align: center; font-size: 18px; color: #f56c6c; margin: 20px 0;">
            检测到紧急情况，请迅速离开现场！
          </p>
          <p style="text-align: center; color: #909399;">
            发生时间：{{ emergencyAlertData.createTime || '未知时间' }}
          </p>
        </template>
      </div>
      <template #footer>
        <el-button @click="emergencyPanelVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3组合式API核心函数
import { ref, computed, onMounted, onUnmounted } from 'vue'
// HTTP客户端用于后端API调用
import axios from 'axios'
// Element Plus图标组件
import { Monitor, DataLine, VideoCamera, Warning, Document, VideoPlay, DataAnalysis } from '@element-plus/icons-vue'
// Element Plus通知组件
import { ElNotification } from 'element-plus'
// WebSocket客户端依赖
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

// 业务功能组件导入
import DeviceList from './components/DeviceList.vue'
import SensorMonitor from './components/SensorMonitor.vue'
import CameraMonitor from './components/CameraMonitor.vue'
import AlertList from './components/AlertList.vue'
import EmergencyPlan from './components/EmergencyPlan.vue'
import EmergencyDrill from './components/EmergencyDrill.vue'
import StatisticsDashboard from './components/StatisticsDashboard.vue'

export default {
  // 组件注册声明
  components: { 
    DeviceList, 
    SensorMonitor, 
    CameraMonitor, 
    AlertList, 
    EmergencyPlan, 
    EmergencyDrill,
    StatisticsDashboard,
    Monitor, 
    DataLine, 
    VideoCamera, 
    Warning, 
    Document,
    VideoPlay,
    DataAnalysis
  },
  
  // Vue3组合式API入口函数
  setup() {
    // ========== 响应式数据声明 ==========
    // 用户认证相关
    const username = ref('admin')
    const password = ref('admin123')
    const isLoggedIn = ref(false)
    const userInfo = ref(null)
    const loginMsg = ref('')
    const activeMenu = ref('devices')
    
    // 应急指挥面板状态
    const emergencyPanelVisible = ref(false)
    const emergencyAlertData = ref(null)
    
    // WebSocket客户端 提前声明 避免在使用时未定义
    let stompClient = null

    // 动态组件映射计算属性
    const currentComponent = computed(() => {
      switch (activeMenu.value) {
        case 'devices': return DeviceList
        case 'sensor': return SensorMonitor
        case 'camera': return CameraMonitor
        case 'alerts': return AlertList
        case 'drill': return EmergencyDrill
        case 'statistics': return StatisticsDashboard
        case 'emergency': return EmergencyPlan
        default: return DeviceList
      }
    })

    // 用户角色提取计算属性，优先从userInfo获取，否则从localStorage获取
    const userRole = computed(() => userInfo.value?.role || localStorage.getItem('userRole'))

    // 动态头像路径计算属性
    const userAvatar = computed(() => {
      // 根据角色返回不同头像
      if (userInfo.value?.role === 'admin') {
        return '/admin-avatar.png'
      } else {
        return '/user-avatar.png'
      }
    })

    // 用户登录业务方法
    const login = async () => {
      try {
        const res = await axios.post('/api/user/login', {
          username: username.value,
          password: password.value
        })
        
        if (res.data.success) {
          isLoggedIn.value = true
          userInfo.value = res.data.data
          localStorage.setItem('userRole', userInfo.value.role)
          loginMsg.value = ''
          connectWebSocket()  // 登录成功后建立WebSocket连接
        } else {
          loginMsg.value = res.data.message
        }
      } catch (error) {
        loginMsg.value = '请求失败 请检查网络'
      }
    }

    // 用户登出业务方法
    const logout = () => {
      isLoggedIn.value = false
      userInfo.value = null
      localStorage.removeItem('userRole')  // 清除用户角色
      if (stompClient) {
        stompClient.deactivate()
        stompClient = null
      }
    }

    // 菜单选择处理方法
    const handleMenuSelect = (index) => {
      activeMenu.value = index
    }

    // 打开应急指挥面板方法
    const openEmergencyPanel = (alertData) => {
      emergencyAlertData.value = alertData
      emergencyPanelVisible.value = true
    }

    // 建立WebSocket连接方法
    const connectWebSocket = () => {
      const wsUrl = process.env.VUE_APP_WS_URL || 'http://localhost:8081/ws'
      const socket = new SockJS(wsUrl)
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        debug: (str) => console.log(str),
        onConnect: (frame) => {
          console.log('WebSocket 连接成功 ' + frame)
          stompClient.subscribe('/topic/alerts', (message) => {
            if (message.body) {
              const alertData = JSON.parse(message.body)
              // 演练告警或AI预警都弹出应急面板
              if (alertData.id === -1 || alertData.alertType?.includes('AI预警')) {
                openEmergencyPanel(alertData)
              } else {
                ElNotification({
                  title: '新告警',
                  message: alertData.content || '传感器检测到异常',
                  type: 'warning',
                  duration: 6000,
                  position: 'top-right'
                })
                window.dispatchEvent(new CustomEvent('new-alert-received', { detail: alertData }))
              }
            }
          })
        },
        onStompError: (error) => {
          console.error('WebSocket 连接失败:', error)
        }
      })
      stompClient.activate()
    }

    // 组件挂载时，恢复登录状态并建立WebSocket连接
    onMounted(() => {
      const savedRole = localStorage.getItem('userRole')
      if (savedRole) {
        // 恢复登录状态
        isLoggedIn.value = true
        userInfo.value = { role: savedRole }
        // 建立WebSocket连接
        connectWebSocket()
      }
    })
    // 组件卸载时断开WebSocket连接
    onUnmounted(() => stompClient && stompClient.deactivate())

    // 返回所有响应式数据和方法供模板使用
    return { 
      username, password, isLoggedIn, userInfo, loginMsg, login,
      logout, activeMenu, handleMenuSelect, currentComponent, userRole,
      emergencyPanelVisible,
      emergencyAlertData,
      openEmergencyPanel,
      userAvatar
    }
  }
}
</script>

<style>
/* 全局样式重置 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

/* 应用容器高度设置 */
#app {
  height: 100vh;
}

/* 登录页面布局 */
.login-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

/* 登录卡片样式 */
.login-card {
  width: 400px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  background: white;
  border: 1px solid #ebeef5;
}

/* 登录标题样式 */
.login-card h2 {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
  font-weight: 600;
  font-size: 24px;
}

/* 登录输入框样式 */
.login-card .el-input {
  margin-bottom: 16px;
}

.login-card .el-input__inner {
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  padding: 12px 16px;
  font-size: 15px;
  height: 48px;
}

.login-card .el-input__inner:focus {
  border-color: #409EFF;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
  outline: none;
}

/* 登录按钮样式 */
.login-card .el-button {
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  padding: 12px;
  background-color: #409EFF;
  border-color: #409EFF;
}

.login-card .el-button:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

/* 登录错误提示样式 */
.login-card p {
  text-align: center;
  margin-top: 8px;
  font-size: 14px;
}

/* 主界面布局 */
.main-layout {
  display: flex;
  height: 100%;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background-color: #1e2a3a;
  color: #bfcbd9;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0,0,0,0.05);
}

/* 应用标题区域样式 */
.logo-area {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  background-color: #1a2634;
  border-bottom: 1px solid #2d3a4b;
}

/* 应用名称样式 */
.app-name {
  font-size: 18px;
  font-weight: 600;
  color: white;
  white-space: nowrap;
  padding: 0 10px;
}

/* 菜单样式 */
.menu {
  flex: 1;
  border-right: none !important;
}

/* 菜单项样式 */
.menu .el-menu-item {
  color: #bfcbd9;
  font-size: 15px;
  font-weight: 500;
  height: 56px;
  line-height: 56px;
  margin: 0 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.menu .el-menu-item:hover {
  background-color: #34495e;
  color: #ffffff;
}

/* 菜单激活状态样式 */
.menu .el-menu-item.is-active {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
}

/* 菜单图标样式 */
.menu .el-menu-item [class^="el-icon"] {
  margin-right: 12px;
  font-size: 18px;
  width: 18px;
  height: 18px;
}

/* 用户信息区域样式 */
.user-info {
  padding: 20px;
  border-top: 1px solid #2d3a4b;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 用户头像样式 */
.user-info .el-avatar {
  flex-shrink: 0;
}

/* 用户详情样式 */
.user-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  overflow: hidden;
}

/* 用户名样式 */
.username {
  font-weight: 500;
  color: white;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 用户角色样式 */
.role {
  font-size: 12px;
  color: #8a9bb5;
}

/* 退出按钮样式 */
.user-info .el-button {
  color: #8a9bb5;
  font-size: 13px;
  padding: 2px 0;
  min-height: auto;
}

.user-info .el-button:hover {
  color: #409EFF;
}

/* 内容区域样式 */
.content {
  flex: 1;
  background-color: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}

/* 页面容器样式 */
.page-container {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  padding: 24px;
  min-height: 100%;
}
</style>
