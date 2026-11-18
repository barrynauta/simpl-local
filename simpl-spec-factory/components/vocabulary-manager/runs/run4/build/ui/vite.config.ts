import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 4433,
    proxy: { '/v1': 'http://localhost:8196' },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
