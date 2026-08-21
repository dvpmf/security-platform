<template>
  <div class="monitor-container">
    <el-row :gutter="20">
      <!-- 左侧设备列表 -->
      <el-col :span="8">
        <el-card class="device-list-card">
          <template #header>
            <span>摄像头设备</span>
          </template>
          <div class="device-list">
            <div
              v-for="camera in cameras"
              :key="camera.id"
              class="device-item"
              :class="{ active: selectedDevice && selectedDevice.id === camera.id }"
              @click="selectDevice(camera)"
            >
              <div>{{ camera.name }} ({{ camera.location }})</div>
              <el-tag :type="camera.status === 1 ? 'success' : 'info'" size="small">
                {{ camera.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </div>
          </div>
          <el-divider />
          <div class="toggle">
            <el-switch
              v-model="useRealCamera"
              active-text="视频监控"
              @change="onCameraModeChange"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧监控画面 -->
      <el-col :span="16">
        <el-card class="monitor-card">
          <template #header>
            <span>{{ selectedDevice ? selectedDevice.name : '请选择设备' }} 实时监控</span>
          </template>
          <div class="video-container">
            <video
              ref="videoElement"
              autoplay
              playsinline
              muted
              class="monitor-video"
              v-show="useRealCamera"
            ></video>
            <div v-if="!useRealCamera" class="placeholder">请开启视频监控</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

// 摄像头列表（硬编码三个）
const cameras = ref([
  { id: 1, name: '大门摄像头', type: 'camera', location: '大门', status: 1 },
  { id: 2, name: '仓库摄像头', type: 'camera', location: '仓库', status: 1 },
  { id: 3, name: '车间摄像头', type: 'camera', location: '车间', status: 1 }
])
const selectedDevice = ref(cameras.value[0])
const useRealCamera = ref(false)  // 默认关闭视频监控
const videoElement = ref(null)
let mediaStream = null

// 选择设备
const selectDevice = (device) => {
  selectedDevice.value = device
  if (useRealCamera.value) {
    // 如果视频监控已开启，重新启动摄像头（刷新画面）
    stopCamera()
    startCamera()
  }
}

// 切换视频监控
const onCameraModeChange = async (value) => {
  if (value) {
    await startCamera()
  } else {
    stopCamera()
  }
}

// 启动摄像头
const startCamera = async () => {
  try {
    stopCamera()
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    mediaStream = stream
    if (videoElement.value) {
      videoElement.value.srcObject = stream
      await videoElement.value.play()
    }
  } catch (err) {
    ElMessage.error('无法访问摄像头：' + err.message)
    useRealCamera.value = false
  }
}

// 停止摄像头
const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
}

onMounted(() => {
  // if (useRealCamera.value) {
  //   startCamera()
  // }
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.monitor-container {
  height: 100%;
}

.device-list-card {
  height: 100%;
  overflow-y: auto;
}

.device-list {
  margin-bottom: 16px;
}

.device-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
  border: 1px solid #ebeef5;
}

.device-item:hover {
  background-color: #f5f7fa;
}

.device-item.active {
  background-color: #409EFF;
  color: white;
  border-color: #409EFF;
}

.device-item.active .el-tag {
  border-color: white;
  color: white;
}

.toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
}

.monitor-card {
  height: 100%;
}

.video-container {
  position: relative;
  height: 400px;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.monitor-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  color: #909399;
  font-size: 16px;
}
</style>
