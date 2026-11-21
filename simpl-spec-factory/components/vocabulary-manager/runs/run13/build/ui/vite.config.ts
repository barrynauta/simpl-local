import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  build: {
    // No inline assets: the Content-Security-Policy forbids inline scripts and styles (D123).
    assetsInlineLimit: 0,
    modulePreload: { polyfill: false },
  },
  server: {
    proxy: { '/v1': 'http://localhost:8196' },
  },
});
