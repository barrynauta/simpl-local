/// <reference types="vite/client" />
declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<object, object, unknown>;
  export default component;
}
interface Window {
  __SM_CONFIG__?: { keycloakUrl: string; realm: string; clientId: string; apiBase: string };
}
