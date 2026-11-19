import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./router";
import { initAuth, type UiConfig } from "./auth";
import "./style.css";

async function start(): Promise<void> {
  const config = (await (await fetch("/config.json")).json()) as UiConfig;
  await initAuth(config);
  createApp(App).use(router).mount("#app");
}

void start();
