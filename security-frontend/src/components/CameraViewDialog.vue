<template>
  <div class="camera-view-dialog">
    <el-row :gutter="20">
      <el-col :span="16">
        <div class="video-container">
          <video
            ref="videoElement"
            autoplay
            playsinline
            muted
            class="monitor-video"
          ></video>
        </div>
      </el-col>
      <el-col :span="8">
        <el-card class="control-card">
          <template #header>控制面板</template>
          <div class="control-item">
            <el-switch
              v-model="cameraEnabled"
              active-text="视频监控"
              @change="onToggle"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const cameraEnabled = ref(false)
const videoElement = ref(null)
let mediaStream = null

const startCamera = async () => {
  try {
    if (mediaStream) {
      stopCamera()
    }
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    mediaStream = stream
    if (videoElement.value) {
      videoElement.value.srcObject = stream
      await videoElement.value.play()
    }
  } catch (err) {
    ElMessage.error('无法访问摄像头：' + err.message)
    cameraEnabled.value = false
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
  if (videoElement.value) {
    videoElement.value.srcObject = null
  }
}

const onToggle = (value) => {
  if (value) {
    startCamera()
  } else {
    stopCamera()
  }
}

onMounted(() => {
  // if (cameraEnabled.value) {
  //   startCamera()
  // }
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.camera-view-dialog {
  padding: 10px;
}
.video-container {
  background-color: #000;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  overflow: hidden;
}
.monitor-video {
  width: 100%;
  height: auto;
  display: block;
}
.control-card {
  background-color: #f5f7fa;
}
.control-item {
  margin-bottom: 15px;
}
</style>