import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  // 打包产物直接写进 Spring Boot 的静态资源目录
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
