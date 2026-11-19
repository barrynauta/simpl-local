/** Runtime configuration served by nginx as /config.json, so ports and URLs are not built into the bundle (E4). */
export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
  apiBase: string;
}

export async function loadConfig(fetcher: typeof fetch = fetch): Promise<UiConfig> {
  const response = await fetcher('/config.json', { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error('The UI configuration could not be loaded.');
  }
  return (await response.json()) as UiConfig;
}
