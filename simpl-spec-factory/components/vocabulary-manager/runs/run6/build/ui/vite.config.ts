/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: { "/v1": "http://localhost:8196", "/config.json": "http://localhost:4433" },
  },
  build: { sourcemap: false, chunkSizeWarningLimit: 1500 },
  test: {
    environment: "jsdom",
    coverage: { provider: "v8", reporter: ["text", "html"], include: ["src/format.ts", "src/turtleCheck.ts", "src/errors.ts", "src/texts.ts", "src/notifications.ts"] },
  },
});
