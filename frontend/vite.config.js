import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: true,
    watch: {
      // 工具原子写文件时会生成 .xxx.tmpdir 临时目录，Windows 上被锁住会让 watcher 抛 EBUSY
      ignored: ['**/.*.tmpdir/**', '**/*.tmp']
    },
    // 前端 5173 -> 后端 8081，走代理可以规避浏览器跨域（CORS）限制
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
