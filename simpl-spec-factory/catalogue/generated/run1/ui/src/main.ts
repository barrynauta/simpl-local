import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import SearchView from './views/SearchView.vue';
import DetailView from './views/DetailView.vue';
import { initAuth } from './auth';
import './style.css';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: SearchView },
    { path: '/details', component: DetailView },
  ],
});

initAuth()
  .then(() => createApp(App).use(router).mount('#app'))
  .catch((e) => {
    document.getElementById('app')!.textContent = `Sign-in failed: ${e}`;
  });
