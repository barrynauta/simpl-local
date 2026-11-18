import { createApp } from 'vue';
import App from './App.vue';
import { router } from './router';
import { loadConfig } from './config';
import { initAuth } from './auth';
import { setApiBase } from './api';
import './style.css';

async function start(): Promise<void> {
  const config = await loadConfig();
  setApiBase(config.apiBase);
  await initAuth(config);
  createApp(App).use(router).mount('#app');
}

start().catch((error: unknown) => {
  const app = document.getElementById('app');
  if (app) {
    app.textContent = `The application could not start: ${error instanceof Error ? error.message : String(error)}`;
  }
});
