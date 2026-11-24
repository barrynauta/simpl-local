import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  build: { outDir: 'dist', sourcemap: false, cssCodeSplit: false },
  test: {
    environment: 'jsdom',
    include: ['tests/**/*.spec.ts'],
  },
});
