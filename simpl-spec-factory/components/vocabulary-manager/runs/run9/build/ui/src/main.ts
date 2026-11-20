import { createApp } from 'vue';
import App from './App.vue';
import { login, type UiConfig } from './auth';
import { router } from './router';
import './style.css';

async function start(): Promise<void> {
  const config = (await (await fetch('/config.json')).json()) as UiConfig;
  await login(config);
  createApp(App).use(router).mount('#app');
}

void start();
