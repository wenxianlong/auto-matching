import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 【绝对不要写 rewrite】
        configure: (proxy, options) => {
          // 【新增】：在终端打印代理日志，证明 Vite 接管了请求
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('🚀 [Vite Proxy] 拦截到请求:', req.method, req.url, '-> 转发到:', options.target);
          });
          proxy.on('error', (err, req, res) => {
            console.error('❌ [Vite Proxy] 代理出错:', err);
          });
        }
      }
    }
  }
})
