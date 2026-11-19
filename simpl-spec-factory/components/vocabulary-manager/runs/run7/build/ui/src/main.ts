import { createApp } from 'vue'
import App from './App.vue'
import { initAuth, type UiConfig } from './lib/auth'
import { router } from './router'
import './styles.css'

async function start() {
  const response = await fetch('/config.json', { cache: 'no-store' })
  const config = (await response.json()) as UiConfig
  await initAuth(config)
  createApp(App).use(router).mount('#app')
}

start().catch((error: unknown) => {
  const app = document.getElementById('app')
  if (app) {
    app.setAttribute('role', 'alert')
    app.textContent = `The application could not start: ${error instanceof Error ? error.message : String(error)}`
  }
})
