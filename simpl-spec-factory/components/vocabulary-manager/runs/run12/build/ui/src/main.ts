import { createApp } from 'vue';
import Keycloak from 'keycloak-js';
import App from './App.vue';
import { configureSession } from './auth';
import { loadConfig } from './config';
import { installPrimeVue } from './primevue';
import { makeRouter } from './router';
import './style.css';

async function start() {
  const config = await loadConfig();
  const keycloak = new Keycloak({
    url: config.keycloakUrl,
    realm: config.keycloakRealm,
    clientId: config.keycloakClientId,
  });
  // D18: a user without a token is sent to the Keycloak login page.
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  const parsed = (keycloak.tokenParsed ?? {}) as {
    preferred_username?: string;
    realm_access?: { roles?: string[] };
  };
  configureSession({
    username: parsed.preferred_username ?? '',
    roles: parsed.realm_access?.roles ?? [],
    tokenProvider: async () => {
      try {
        await keycloak.updateToken(30);
      } catch {
        await keycloak.login();
      }
      return keycloak.token;
    },
    logout: () => void keycloak.logout({ redirectUri: `${window.location.origin}/vocabularies` }),
  });

  const app = createApp(App);
  installPrimeVue(app);
  app.use(makeRouter());
  app.mount('#app');
}

void start();
