import { createApp } from 'vue';
import PrimeVue from 'primevue/config';
import App from './App.vue';
import { initKeycloak, loadConfig, setSession } from './auth';
import { makeRouter } from './router';
import './style.css';

async function start(): Promise<void> {
  const config = await loadConfig();
  setSession(await initKeycloak(config));
  const app = createApp(App);
  app.use(PrimeVue, { unstyled: true });
  app.use(makeRouter());
  app.mount('#app');
}

void start();
