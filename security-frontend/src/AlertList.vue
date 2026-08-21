import { ref, onMounted, onUnmounted, computed } from 'vue'

export default {
  setup() {
    const alerts = ref([])

    const fetchAlerts = async () => {
      try {
        const response = await fetch('/api/alerts')
        alerts.value = await response.json()
      } catch (error) {
        console.error('Failed to fetch alerts:', error)
      }
    }

    // 监听新告警事件，自动刷新列表
    const onNewAlert = () => {
      fetchAlerts()
    }

    onMounted(() => {
      fetchAlerts()
      window.addEventListener('new-alert-received', onNewAlert)
    })

    onUnmounted(() => {
      window.removeEventListener('new-alert-received', onNewAlert)
    })

    const sortedAlerts = computed(() => {
      return alerts.value.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
    })

    return {
      alerts: sortedAlerts,
    }
  },
}
