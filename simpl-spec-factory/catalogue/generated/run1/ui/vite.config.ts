import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 18104,
    proxy: {
      '/api/v1': { target: 'http://localhost:18103', rewrite: (p) => p.replace(/^\/api/, '') },
      '/api/v2': { target: 'http://localhost:18103', rewrite: (p) => p.replace(/^\/api/, '') },
    },
  },
});
