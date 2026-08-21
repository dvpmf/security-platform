<template>
  <div class="sensor-monitor">
    <!-- 设备选择下拉框 -->
    <el-select 
      v-model="selectedDevice" 
      placeholder="选择传感器设备" 
      @change="fetchData" 
      clearable 
      style="width: 300px; margin-bottom: 20px;"
    >
      <el-option 
        v-for="d in devices" 
        :key="d.id" 
        :label="d.name + ' - ' + typeMap[d.type]" 
        :value="d.id" 
      />
    </el-select>

    <!-- 加载状态 -->
    <div v-if="loading" class="status-message">加载中...</div>
    
    <!-- 无数据提示 -->
    <div v-else-if="!latestData && selectedDevice" class="status-message">
      暂无最新数据 请等待传感器采集
    </div>

    <!-- 最新数据显示 -->
    <div v-if="latestData" class="latest-data">
      <h3>最新数据</h3>
      <p><span class="label">数值：</span>{{ latestData.value.toFixed(2) }} {{ latestData.unit }}</p>
      <p><span class="label">时间：</span>{{ formatTime(latestData.createTime) }}</p>
    </div>

    <!-- AI 智能分析结果 -->
    <div v-if="aiAnalysis.valid" class="ai-analysis-card" :class="aiAnalysis.levelClass">
      <div class="ai-title">
        <el-icon><Cpu /></el-icon> AI 智能分析
      </div>
      <div class="ai-description">{{ aiAnalysis.description }}</div>
      <div class="ai-meta">
        最新值：{{ aiAnalysis.latestValue.toFixed(2) }} {{ aiAnalysis.unit }}
      </div>
    </div>
    <div v-else-if="aiAnalysis.message" class="ai-analysis-card info">
      <div class="ai-title">AI 智能分析</div>
      <div class="ai-description">{{ aiAnalysis.message }}</div>
    </div>

    <!-- 历史数据曲线图 -->
    <div id="chart" style="width:100%; height:400px; margin-top:20px;"></div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  components: { Cpu },
  setup() {
    // 所有传感器设备
    const devices = ref([])
    // 当前选中设备ID
    const selectedDevice = ref(null)
    // 最新数据
    const latestData = ref(null)
    // 加载状态
    const loading = ref(false)
    // ECharts实例
    let chart = null

    // 类型映射
    const typeMap = {
      smoke: '烟感',
      temperature: '温感'
    }

    // AI 分析结果
    const aiAnalysis = ref({
      valid: false,
      levelClass: 'normal',
      description: '',
      latestValue: 0,
      unit: '',
      message: ''
    })

    // 格式化时间 可根据需要调整
    const formatTime = (timeStr) => {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN', { hour12: false })
    }

    // 获取所有传感器设备 排除摄像头
    const fetchDevices = async () => {
      try {
        const res = await axios.get('/api/device/list')
        devices.value = res.data.filter(d => d.type !== 'camera')
      } catch (error) {
        console.error('获取设备列表失败', error)
      }
    }

    // 获取 AI 分析结果
    const fetchAIAnalysis = async (deviceId) => {
      try {
        const res = await axios.get(`/api/sensor/ai-analysis/${deviceId}`)
        const data = res.data
        // 将 level 字符串映射为样式类名
        if (data.valid) {
          // 后端返回的level已经是字符串 normal warning danger
          data.levelClass = data.level || 'normal'
        }
        aiAnalysis.value = data
      } catch (error) {
        console.error('获取 AI 分析失败', error)
        aiAnalysis.value = {
          valid: false,
          message: 'AI 分析服务暂时不可用'
        }
      }
    }

    // 根据选中设备获取最新数据和历史数据
    const fetchData = async () => {
      if (!selectedDevice.value) return
      loading.value = true
      try {
        // 并发请求最新和历史数据
        const [latestRes, historyRes] = await Promise.all([
          axios.get(`/api/sensor/latest/${selectedDevice.value}`),
          axios.get(`/api/sensor/history/${selectedDevice.value}`)
        ])

        latestData.value = latestRes.data
        const history = historyRes.data

        // 获取 AI 分析结果
        await fetchAIAnalysis(selectedDevice.value)

        // 提取时间和数值
        const times = history.map(item => formatTime(item.createTime))
        const values = history.map(item => item.value)

        // 更新图表
        if (chart) {
          chart.setOption({
            title: {
              text: devices.value.find(d => d.id === selectedDevice.value)?.name + ' 历史数据趋势',
              left: 'center'
            },
            xAxis: {
              data: times,
              // 自动间隔
              axisLabel: { rotate: 30, interval: Math.ceil(times.length / 10) }
            },
            series: [{ data: values }]
          })
        }
      } catch (error) {
        ElMessage.error('传感器数据加载失败')
        console.error('获取传感器数据失败', error)
      } finally {
        loading.value = false
      }
    }

    // 初始化图表
    onMounted(() => {
      fetchDevices()
      chart = echarts.init(document.getElementById('chart'))
      chart.setOption({
        title: { text: '历史数据趋势', left: 'center' },
        tooltip: { trigger: 'axis' },
        grid: { left: '5%', right: '5%', bottom: '10%', top: '15%', containLabel: true },
        xAxis: {
          type: 'category',
          name: '时间',
          axisLabel: { rotate: 30 }
        },
        yAxis: {
          type: 'value',
          name: '数值'
        },
        series: [{
          name: '传感器数值',
          type: 'line',
          data: [],
          smooth: true,
          symbol: 'circle',
          lineStyle: { width: 2, color: '#409EFF' }
        }]
      })
    })

    // 监听选中设备变化 自动获取数据
    watch(selectedDevice, fetchData)

    return {
      devices,
      selectedDevice,
      latestData,
      loading,
      typeMap,
      formatTime,
      fetchData,
      aiAnalysis
    }
  }
}
</script>

<style scoped>
.sensor-monitor {
  padding: 10px;
}
.status-message {
  margin: 20px 0;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
  text-align: center;
  color: #909399;
}
.latest-data {
  margin: 20px 0;
  padding: 15px 20px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}
.latest-data h3 {
  margin: 0 0 10px 0;
  color: #1f2d3d;
}
.latest-data p {
  margin: 5px 0;
  color: #2c3e50;
}
.latest-data .label {
  font-weight: 500;
  color: #5e6e82;
  width: 60px;
  display: inline-block;
}

.ai-analysis-card {
  margin: 20px 0;
  padding: 15px 20px;
  border-radius: 8px;
  border-left: 4px solid;
  background-color: #fff;
}
.ai-analysis-card.normal {
  border-left-color: #67c23a;
  background-color: #f0f9eb;
}
.ai-analysis-card.warning {
  border-left-color: #e6a23c;
  background-color: #fdf6ec;
}
.ai-analysis-card.danger {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}
.ai-analysis-card.info {
  border-left-color: #909399;
  background-color: #f4f4f5;
}
.ai-title {
  font-weight: 600;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ai-description {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}
.ai-meta {
  font-size: 12px;
  color: #888;
}
</style>
