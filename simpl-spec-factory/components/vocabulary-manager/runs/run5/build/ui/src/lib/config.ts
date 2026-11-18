/** Runtime configuration, served as /config.json and filled from the environment (decision E4). */
export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

export async function loadConfig(): Promise<UiConfig> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('The UI configuration could not be loaded.');
  }
  return (await response.json()) as UiConfig;
}
