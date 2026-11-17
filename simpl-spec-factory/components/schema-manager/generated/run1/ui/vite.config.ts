import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// In development the API is proxied the same way nginx does it in the container.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 4334,
    proxy: {
      '/v1': 'http://localhost:8098',
    },
  },
});
