import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 4200,
    proxy: { '/v1': 'http://localhost:8196' },
  },
  build: { sourcemap: false, chunkSizeWarningLimit: 1200 },
  test: { environment: 'jsdom' },
})
