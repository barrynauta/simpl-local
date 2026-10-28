import { createApp } from 'vue';
import App from './App.vue';
import { router } from './router';
import { initAuth } from './auth';
import './style.css';

initAuth()
  .then(() => {
    createApp(App).use(router).mount('#app');
  })
  .catch((e) => {
    const el = document.getElementById('app');
    if (el) {
      el.textContent = 'Sign-in failed. Please reload the page.';
    }
    console.error(e);
  });
