export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
  apiBase: string;
}

let current: UiConfig = {
  keycloakUrl: 'http://localhost:8193',
  realm: 'simpl',
  clientId: 'vocabulary-manager-ui',
  apiBase: '/v1',
};

/** Loads /config.json, the runtime configuration of the deployment. */
export async function loadConfig(): Promise<UiConfig> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (response.ok) {
    current = { ...current, ...((await response.json()) as Partial<UiConfig>) };
  }
  return current;
}

export function config(): UiConfig {
  return current;
}
