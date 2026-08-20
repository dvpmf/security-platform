export default {
  methods: {
    renderCharts() {
      const chart = this.$refs.chart;
      if (!chart) return;

      const option = {
        title: {
          text: '设备告警分布分析',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: '10%',
          data: ['仓库温感', '仓库烟感', '车间温感', '车间烟感', '设备异常']
        },
        series: [
          {
            name: '告警分布',
            type: 'pie',
            radius: '55%',
            center: ['50%', '60%'],
            data: [
              { value: this.warehouseTempCount, name: '仓库温感' },
              { value: this.warehouseSmokeCount, name: '仓库烟感' },
              { value: this.workshopTempCount, name: '车间温感' },
              { value: this.workshopSmokeCount, name: '车间烟感' },
              { value: this.deviceAbnormalCount, name: '设备异常' }
            ],
            itemStyle: {
              color: function(params) {
                // 根据名称返回对应颜色
                switch (params.name) {
                  case '仓库温感':
                    return '#409eff';
                  case '仓库烟感':
                    return '#67c23a';
                  case '车间温感':
                    return '#e6a23c';
                  case '车间烟感':
                    return '#f56c6c';
                  case '设备异常':
                    return '#9b59b6'; 
                  default:
                    return '#999';
                }
              }
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };

      chart.setOption(option);
    }
  }
}