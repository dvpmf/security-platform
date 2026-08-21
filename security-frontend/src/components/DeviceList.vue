<template>
  <div>
    <!-- 页面头部 -->
    <div class="list-header">
      <h2 class="page-title">设备管理</h2>
      <el-button v-if="userRole === 'admin'" type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon> 添加设备
      </el-button>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon"><el-icon><Camera /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">摄像头</div>
            <div class="stat-value">{{ cameraCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon"><el-icon><Smoking /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">烟感</div>
            <div class="stat-value">{{ smokeCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon"><el-icon><HotWater /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">温感</div>
            <div class="stat-value">{{ tempCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">在线率</div>
            <div class="stat-value">{{ onlineRate }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备表格 -->
    <el-table :data="devices" stripe border>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'camera' ? '' : row.type === 'smoke' ? 'warning' : 'danger'" effect="plain">
            {{ typeMap[row.type] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="位置" min-width="120" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark" size="small">
            {{ row.status === 1 ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center">
        <template #default="{ row }">
          <el-button v-if="userRole === 'admin'" size="small" type="primary" plain @click="editDevice(row)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button v-if="userRole === 'admin'" size="small" type="danger" plain @click="deleteDevice(row.id)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
          <el-button 
            v-if="row.type === 'camera'"
            size="small" 
            type="info" 
            plain 
            @click="viewDevice(row)"
          >
            <el-icon><InfoFilled /></el-icon> 查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 对话框部分 -->
    <!-- 添加/编辑对话框 仅管理员可见 通过v-if控制不渲染 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" v-if="userRole === 'admin'">
      <el-form :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="摄像头" value="camera" />
            <el-option label="烟感" value="smoke" />
            <el-option label="温感" value="temperature" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveDevice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 摄像头查看对话框 所有用户可见 -->
    <el-dialog v-model="cameraDialogVisible" title="摄像头画面" width="600px">
      <img src="/camera-placeholder.jpg" style="width:100%" alt="模拟摄像头画面" />
    </el-dialog>

    <!-- 设备查看对话框 -->
    <el-dialog
      v-model="deviceViewDialogVisible"
      :title="`设备详情 - ${currentDevice?.name || ''}`"
      width="800px"
      destroy-on-close
    >
      <camera-view-dialog
        v-if="currentDevice && currentDevice.type === 'camera'"
        :device-id="currentDevice.id"
        :device-name="currentDevice.name"
      />
      <div v-else-if="currentDevice">
        <el-alert type="info" :closable="false">
          该设备不是摄像头 暂不支持实时画面查看
        </el-alert>
        <p>设备类型：{{ typeMap[currentDevice.type] || currentDevice.type }}</p>
        <p>设备位置：{{ currentDevice.location }}</p>
        <p>设备状态：{{ currentDevice.status === 1 ? '在线' : '离线' }}</p>
      </div>
      <template #footer>
        <el-button @click="deviceViewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
// Element Plus 图标组件
import { Plus, Camera, Smoking, HotWater, CircleCheck, Edit, Delete, InfoFilled } from '@element-plus/icons-vue'
import CameraViewDialog from './CameraViewDialog.vue'

export default {
  props: {
    userRole: {
      type: String,
      required: true
    }
  },
  components: {
    Plus, Camera, Smoking, HotWater, CircleCheck, Edit, Delete, InfoFilled, CameraViewDialog
  },
  setup(props) {
    // 设备列表
    const devices = ref([])
    // 控制添加/编辑对话框显示
    const dialogVisible = ref(false)
    // 控制摄像头查看对话框显示
    const cameraDialogVisible = ref(false)
    // 控制设备查看对话框显示
    const deviceViewDialogVisible = ref(false)
    // 当前查看的设备
    const currentDevice = ref(null)
    // 对话框标题
    const dialogTitle = ref('添加设备')
    // 表单数据
    const form = ref({
      id: null,
      name: '',
      type: 'camera',
      location: '',
      status: 1
    })
    // 类型映射 用于显示中文
    const typeMap = {
      camera: '摄像头',
      smoke: '烟感',
      temperature: '温感'
    }

    // 统计计算属性
    const cameraCount = computed(() => devices.value.filter(d => d.type === 'camera').length)

    const smokeCount = computed(() => devices.value.filter(d => d.type === 'smoke').length)

    const tempCount = computed(() => devices.value.filter(d => d.type === 'temperature').length)

    const onlineCount = computed(() => devices.value.filter(d => d.status === 1).length)

    const onlineRate = computed(() => devices.value.length ? ((onlineCount.value / devices.value.length) * 100).toFixed(0) : 0)

    // 获取设备列表 调用后端接口
    const fetchDevices = async () => {
      try {
        const res = await axios.get('/api/device/list')
        devices.value = res.data
      } catch (error) {
        ElMessage.error('设备列表加载失败 请检查网络')
      }
    }

    // 显示添加对话框
    const showAddDialog = () => {
      if (props.userRole !== 'admin') {
        ElMessage.warning('您没有权限')
        return
      }
      dialogTitle.value = '添加设备'
      form.value = { id: null, name: '', types: ['camera'], location: '', status: 1 }
      dialogVisible.value = true
    }

    // 编辑设备 填充表单
    const editDevice = (row) => {
      if (props.userRole !== 'admin') return
      dialogTitle.value = '编辑设备'
      // 确保types字段是数组格式
      const editedRow = { ...row }
      if (!Array.isArray(editedRow.types)) {
        editedRow.types = editedRow.type ? [editedRow.type] : []
        delete editedRow.type
      }
      form.value = editedRow
      dialogVisible.value = true
    }

    // 保存设备 新增或更新
    const saveDevice = async () => {
      if (props.userRole !== 'admin') return
      if (form.value.id) {
        await axios.put('/api/device/update', form.value)
      } else {
        await axios.post('/api/device/add', form.value)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      // 刷新列表
      fetchDevices()
    }

    // 删除设备
    const deleteDevice = async (id) => {
      if (props.userRole !== 'admin') return
      await axios.delete(`/api/device/delete/${id}`)
      ElMessage.success('删除成功')
      fetchDevices()
    }

    // 查看摄像头
    // eslint-disable-next-line no-unused-vars
   const viewCamera = (_row) => {
      cameraDialogVisible.value = true
    }

    // 查看设备详情
    const viewDevice = (row) => {
      currentDevice.value = row
      deviceViewDialogVisible.value = true
    }

    // 组件挂载时获取设备列表
    onMounted(() => {
      fetchDevices()
    })

    return { 
      devices, dialogVisible, cameraDialogVisible, deviceViewDialogVisible, currentDevice, dialogTitle, form, typeMap,
      showAddDialog, editDevice, saveDevice, deleteDevice, viewCamera, viewDevice,
      cameraCount, smokeCount, tempCount, onlineRate
    }
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

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
  margin: 0 0 24px 0;
}

.stat-cards {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
}

.stat-card {
  height: 150px;
  cursor: pointer;
  transition: transform 0.2s ease;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 12px;
  color: #409EFF;
}

.stat-info {
  text-align: center;
}

.stat-label {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #333;
}

.stat-card .el-icon {
  margin-right: 0;
}

.stat-card .stat-icon {
  margin-bottom: 20px;
}

.stat-card .stat-label {
  margin-bottom: 10px;
}

.monitor-card {
  border-radius: 8px;
  padding: 16px;
}

.latest-data {
  display: flex;
  gap: 40px;
  margin: 20px 0;
  background-color: #f9fafc;
  padding: 16px 24px;
  border-radius: 8px;
}

.data-item {
  display: flex;
  flex-direction: column;
}

.data-label {
  font-size: 13px;
  color: #5e6e82;
  margin-bottom: 4px;
}

.data-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2d3d;
}

.data-time {
  font-size: 16px;
  color: #2c3e50;
}

.device-view-dialog .el-dialog__body {
  padding: 20px;
}
</style>
