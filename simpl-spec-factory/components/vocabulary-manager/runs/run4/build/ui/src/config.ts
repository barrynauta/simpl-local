/** Runtime configuration, served by nginx from the environment as /config.json (MACH-07). */
export interface AppConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
  apiBase: string;
}

export async function loadConfig(): Promise<AppConfig> {
  const response = await fetch('/config.json', { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error('The UI configuration could not be loaded.');
  }
  return (await response.json()) as AppConfig;
}
