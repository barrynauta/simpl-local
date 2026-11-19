import { createApp } from 'vue';
import App from './App.vue';
import { VocabularyApi } from './api';
import { startSession } from './auth';
import { loadConfig } from './config';
import { APP_CONTEXT, createContext } from './context';
import { createAppRouter } from './router';
import './style.css';

async function bootstrap() {
  const config = await loadConfig();
  const session = await startSession(config);
  const api = new VocabularyApi(config.apiBase, () => session.token());
  const app = createApp(App);
  app.provide(APP_CONTEXT, createContext(api, session));
  app.use(createAppRouter());
  app.mount('#app');
}

bootstrap().catch(() => {
  const root = document.getElementById('app');
  if (root) {
    root.innerHTML = '<p role="alert">Something went wrong. Retry later or contact support.</p>';
  }
});
