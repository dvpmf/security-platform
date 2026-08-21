<template>
  <div>
    <div class="list-header">
      <div class="title-area">
        <h2 class="page-title">告警记录</h2>
      </div>
      <div class="action-buttons">
        <el-button v-if="userRole === 'admin'" type="danger" @click="manualAlarm">
          <el-icon><Warning /></el-icon> 手动触发告警
        </el-button>
        <el-button v-if="userRole === 'admin'" type="warning" @click="handleAllAlerts">一键处理所有</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon warning"><el-icon><InfoFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">预警</div>
            <div class="stat-value">{{ warningCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon normal"><el-icon><InfoFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">一般</div>
            <div class="stat-value">{{ normalCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon danger"><el-icon><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">严重</div>
            <div class="stat-value">{{ severeCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon success"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已处理</div>
            <div class="stat-value">{{ handledCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon info"><el-icon><InfoFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">未处理</div>
            <div class="stat-value">{{ unhandledCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="alerts" stripe border>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="alertType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag 
            :class="{
              'sensor-warning': (row.alertType.includes('烟雾') || row.alertType.includes('高温'))
            }"
            :type="row.alertType === '设备异常' ? '' : 
                   (row.alertType.includes('烟雾') || row.alertType.includes('高温') ? 'warning' : '')"
            effect="plain"
          >
            {{ row.alertType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="200" />
      <el-table-column prop="level" label="级别" width="80" align="center">
        <template #default="{ row }">
          <el-tag 
            :class="{
              'level-warning': row.level === 1,
              'level-normal': row.level === 2,
              'level-severe': row.level === 3
            }"
            effect="dark"
            size="small"
          >
            {{ row.level === 3 ? '严重' : (row.level === 2 ? '一般' : '预警') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'info' : 'success'" effect="dark" size="small">
            {{ row.status === 0 ? '未处理' : '已处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" />
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="userRole === 'admin' && row.status === 0" size="small" type="primary" plain @click="handleAlert(row.id)">
            <el-icon><Check /></el-icon> 处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import { Check, CircleCheckFilled, Warning, InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'AlertList',
  components: { Check, CircleCheckFilled, Warning, InfoFilled },
  props: { userRole: String },
  setup() {
    const alerts = ref([])

    const fetchAlerts = async () => {
      try {
        const res = await axios.get('/api/alert/list')
        alerts.value = res.data
      } catch (err) {
        ElMessage.error('告警列表加载失败')
        console.error('获取告警列表失败', err)
      }
    }

    const handleAlert = async (id) => {
      try {
        await axios.put(`/api/alert/handle/${id}?handler=admin`)
        ElMessage.success('已处理')
        fetchAlerts()
      } catch (error) {
        ElMessage.error('处理失败')
      }
    }

    const handleAllAlerts = async () => {
      try {
        const res = await axios.put('/api/alert/handleAll?handler=admin')
        ElMessage.success(res.data.message || '已处理所有告警')
        fetchAlerts()
      } catch (error) {
        ElMessage.error('请求失败')
      }
    }

    const manualAlarm = async () => {
      try {
        const res = await axios.post('/api/alert/manual/batch')
        const newAlerts = res.data
        newAlerts.forEach((alert, index) => {
          setTimeout(() => {
            ElNotification({
              title: '新告警',
              message: alert.content || `${alert.alertType}：${alert.detail}`,
              type: 'warning',
              duration: 5000,
              position: 'top-right'
            })
          }, index * 500)
        })
        fetchAlerts()
      } catch (error) {
        ElMessage.error('触发失败：' + (error.response?.data?.message || error.message))
      }
    }

    const unhandledCount = computed(() => alerts.value.filter(a => a.status === 0).length)
    const handledCount = computed(() => alerts.value.filter(a => a.status === 1).length)
    const warningCount = computed(() => alerts.value.filter(a => a.level === 1).length)
    const normalCount = computed(() => alerts.value.filter(a => a.level === 2).length)
    const severeCount = computed(() => alerts.value.filter(a => a.level === 3).length)

    onMounted(() => { fetchAlerts() })

    return { alerts, manualAlarm, handleAlert, handleAllAlerts, unhandledCount, handledCount, warningCount, normalCount, severeCount }
  }
}
</script>

<style scoped>
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.title-area h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}
.action-buttons {
  display: flex;
  gap: 12px;
}
.stat-cards {
  margin-bottom: 24px;
}
.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-icon {
  font-size: 32px;
  margin-right: 16px;
}
.stat-icon.info {
  color: #909399;
}
.stat-icon.warning {
  color: #409eff;
}
.stat-icon.success {
  color: #67c23a;
}
.stat-icon.danger {
  color: #f56c6c;
}
.stat-icon.normal {
  color: #e6a23c;
}
.stat-info {
  flex: 1;
}
.stat-label {
  font-size: 14px;
  color: #5e6e82;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.2;
}

.level-warning {
  background-color: #409EFF;
  border-color: #409EFF;
}
.level-normal {
  background-color: #E6A23C;
  border-color: #E6A23C;
}
.level-severe {
  background-color: #F56C6C;
  border-color: #F56C6C;
}

.sensor-warning {
  background-color: #fffbe8 !important;
  border-color: #e6a23c !important;
  color: #e6a23c !important;
}
</style>
