import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/agora-aigccloud-example/',
  plugins: [vue()],
  assetsInclude: ['**/*.proto'], // 将 .proto 文件视为资源文件
  server: {
    proxy: {
      '/api': {
        target: 'https://aigc-aliyun.agoramdn.com',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
