import { createApp } from 'vue';
import PrimeVue from 'primevue/config';
import App from './App.vue';
import { initAuth } from './lib/auth';
import { loadConfig } from './lib/config';
import { makeRouter } from './router';
import './style.css';

async function start(): Promise<void> {
  await loadConfig();
  await initAuth();
  const app = createApp(App);
  app.use(PrimeVue, { unstyled: true });
  app.use(makeRouter());
  app.mount('#app');
}

void start();
