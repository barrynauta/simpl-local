import '@picocss/pico/css/pico.min.css'
import './style.css'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { loadConfig } from './config'
import { initAuth } from './auth'

async function start() {
  await loadConfig()
  await initAuth()
  createApp(App).use(router).mount('#app')
}

start()
