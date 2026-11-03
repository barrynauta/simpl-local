import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Overview from './views/Overview.vue'
import Details from './views/Details.vue'
import Callback from './views/Callback.vue'
import './style.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Overview },
    { path: '/contracts/:nid', component: Details, props: true },
    { path: '/contractOffers/:nid', component: Details, props: true },
    { path: '/callback', component: Callback },
  ],
})

createApp(App).use(router).mount('#app')
