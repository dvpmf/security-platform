<template>
  <div class="statistics-container">
    <h2 class="page-title">告警分析</h2>

    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">总告警数</div>
            <div class="stat-value">{{ statistics.total || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon warning"><el-icon><InfoFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">预警告警</div>
            <div class="stat-value">{{ statistics.warningCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon normal"><el-icon><InfoFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">一般告警</div>
            <div class="stat-value">{{ statistics.normalCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon danger"><el-icon><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">严重告警</div>
            <div class="stat-value">{{ statistics.severeCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>告警趋势分析（近7天）</span></template>
          <div ref="trendChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>设备告警分布分析</span></template>
          <div ref="typeChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { Warning, InfoFilled } from '@element-plus/icons-vue'

const statistics = ref({ total: 0, unhandled: 0, handled: 0, warningCount: 0, normalCount: 0, severeCount: 0, trendDates: [], trendCounts: [], typeDistribution: [] })

const trendChartRef = ref(null)
const typeChartRef = ref(null)
let trendChart = null
let typeChart = null

const renderCharts = () => {
  if (trendChart) trendChart.dispose()
  if (typeChart) typeChart.dispose()
  if (!trendChartRef.value || !typeChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)
  typeChart = echarts.init(typeChartRef.value)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: statistics.value.trendDates, name: '日期' },
    yAxis: { type: 'value', name: '告警数量' },
    series: [{
      data: statistics.value.trendCounts,
      type: 'line',
      smooth: true,
      lineStyle: { color: '#409EFF', width: 2 },
      areaStyle: { opacity: 0.1, color: '#409EFF' }
    }]
  })

  // 设备告警分布饼图
  typeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 'bottom', left: 'center' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: statistics.value.typeDistribution,
      label: { show: true, formatter: '{b}: {d}%' },
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9C27B0']
    }]
  })
}

const loadStatistics = async () => {
  try {
    const res = await axios.get('/api/alert/statistics')
    statistics.value = res.data
    renderCharts()
  } catch (error) {
    console.error('获取统计失败', error)
  }
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', () => {
    if (trendChart) trendChart.resize()
    if (typeChart) typeChart.resize()
  })
})

onUnmounted(() => {
  if (trendChart) trendChart.dispose()
  if (typeChart) typeChart.dispose()
})
</script>

<style scoped>
.statistics-container { padding: 0; }
.page-title { font-size: 20px; font-weight: 600; margin-bottom: 24px; }
.stat-cards { margin-bottom: 24px; }
.stat-card { border-radius: 12px; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-icon { font-size: 32px; margin-right: 16px; color: #409EFF; }
.stat-icon.warning { color: #409eff; }
.stat-icon.success { color: #67c23a; }
.stat-icon.danger { color: #f56c6c; }
.stat-icon.normal { color: #e6a23c; }
.stat-info { flex: 1; }
.stat-label { font-size: 14px; color: #5e6e82; }
.stat-value { font-size: 28px; font-weight: 600; color: #1f2d3d; }
.chart-card { border-radius: 12px; margin-bottom: 20px; }
</style>