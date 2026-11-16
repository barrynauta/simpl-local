/// <reference types="vite/client" />
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
interface Window {
  __CONFIG__?: {
    keycloakUrl: string
    realm: string
    clientId: string
    partyRole: 'PROVIDER' | 'CONSUMER'
    partyName: string
  }
}
