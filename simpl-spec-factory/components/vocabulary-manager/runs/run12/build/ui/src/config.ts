export interface AppConfig {
  keycloakUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

export const DEFAULT_CONFIG: AppConfig = {
  keycloakUrl: 'http://localhost:8193',
  keycloakRealm: 'simpl',
  keycloakClientId: 'vocabulary-manager-ui',
};

/** Loads the runtime configuration that nginx renders from environment variables. */
export async function loadConfig(): Promise<AppConfig> {
  try {
    const res = await fetch('/config.json', { cache: 'no-store' });
    if (!res.ok) return DEFAULT_CONFIG;
    const body = (await res.json()) as Partial<AppConfig>;
    return { ...DEFAULT_CONFIG, ...body };
  } catch {
    return DEFAULT_CONFIG;
  }
}
