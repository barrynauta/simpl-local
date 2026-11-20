import { createApp } from 'vue';
import App from './App.vue';
import { createAppRouter } from './router';
import { signIn, type UiConfig } from './auth';
import './style.css';

async function start(): Promise<void> {
  const config = (await (await fetch('/config.json')).json()) as UiConfig;
  await signIn(config);
  createApp(App).use(createAppRouter()).mount('#app');
}

void start();
