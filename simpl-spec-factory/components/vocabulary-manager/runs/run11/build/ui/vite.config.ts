/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  build: { sourcemap: false, cssCodeSplit: false },
  test: { environment: "jsdom", include: ["tests/**/*.spec.ts"] },
});
