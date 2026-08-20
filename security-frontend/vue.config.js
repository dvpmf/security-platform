module.exports = {
  devServer: {
    port: 8082,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8081',  // 后端 WebSocket 端口
        ws: true,
        changeOrigin: true
      }
    }
  }
}