<template>
  <div class="drill-container">
    <div class="page-header">
      <h2 class="page-title">应急演练</h2>
      <el-button v-if="isAdmin" type="primary" @click="startDrill" :loading="sending">
        <el-icon><VideoPlay /></el-icon> 开始演练
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="drill-card">
          <template #header><span>演练说明</span></template>
          <div class="drill-desc">
            <p>点击开始演练将生成一条模拟告警 系统会通过 WebSocket 实时推送给所有在线用户</p>
            <p>模拟告警会随机匹配一个应急预案 并自动弹出处理步骤</p>
            <p>你可以打开多个浏览器窗口 观察告警实时推送和预案联动效果</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="drill-card">
          <template #header><span>实时演练消息</span></template>
          <div class="drill-messages">
            <div v-for="msg in messages" :key="msg.id" class="message-item">
              <el-tag :type="msg.type" size="small">{{ msg.tag }}</el-tag>
              <span>{{ msg.content }}</span>
              <span class="time">{{ msg.time }}</span>
            </div>
            <div v-if="messages.length === 0" class="empty-tip">暂无演练消息 点击开始演练</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="drill-card" style="margin-top: 20px;">
      <template #header><span>真实告警参考 最近5条</span></template>
      <el-table :data="recentAlerts" stripe size="small">
        <el-table-column prop="alertType" label="类型" width="100" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="160" />
      </el-table>
    </el-card>

  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'

const props = defineProps({
  userRole: {
    type: String,
    default: 'user'
  }
})
const isAdmin = computed(() => props.userRole === 'admin')

const sending = ref(false)
const messages = ref([])
const recentAlerts = ref([])

const addMessage = (msg) => {
  messages.value.unshift(msg)
  if (messages.value.length > 20) messages.value.pop()
}

const startDrill = async () => {
  if (!isAdmin.value) {
    ElMessage.warning('无权限')
    return
  }
  sending.value = true
  try {
    const res = await axios.post('/api/alert/mock')
    ElMessage.success('演练告警已发送')
    // 手动添加本地消息 不用依赖 WebSocket
    addMessage({
      id: Date.now(),
      type: 'warning',
      tag: '演练',
      content: res.data.content || '演练告警已发出',
      time: new Date().toLocaleTimeString()
    })
  } catch (error) {
    ElMessage.error('发送失败：' + error.message)
  } finally {
    sending.value = false
  }
}

const fetchRecentAlerts = async () => {
  try {
    const res = await axios.get('/api/alert/list')
    // 按时间倒序取最近5条
    const sorted = [...res.data].sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    recentAlerts.value = sorted.slice(0, 5)
  } catch (error) {
    console.error('获取告警失败', error)
  }
}

onMounted(() => {
  fetchRecentAlerts()
})
</script>

<style scoped>
.drill-container { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; color: #1f2d3d; }
.drill-card { border-radius: 12px; }
.drill-desc p { margin: 8px 0; color: #5e6e82; line-height: 1.6; }
.drill-messages { max-height: 300px; overflow-y: auto; }
.message-item { padding: 8px 0; border-bottom: 1px solid #ebeef5; display: flex; align-items: center; gap: 12px; }
.message-item .time { margin-left: auto; font-size: 12px; color: #909399; }
.empty-tip { text-align: center; color: #909399; padding: 20px; }
</style>
