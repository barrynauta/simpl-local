import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Overview from './views/Overview.vue'
import Details from './views/Details.vue'
import { initAuth } from './auth'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Overview },
    { path: '/contracts/:nid', component: Details, props: true },
    { path: '/contractOffers/:nid', component: Details, props: true }
  ]
})

initAuth()
  .then(() => createApp(App).use(router).mount('#app'))
  .catch((e) => {
    document.getElementById('app')!.innerHTML =
      '<p data-testid="error-message">Sign-in is not available. Please try again later.</p>'
    console.error(e)
  })
