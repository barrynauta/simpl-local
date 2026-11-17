export interface AppConfig {
  authority: string
  clientId: string
  apiBase: string
  referenceBase: string
}

let config: AppConfig = {
  authority: 'http://localhost:8183/realms/simpl',
  clientId: 'vocabulary-ui',
  apiBase: '/v1',
  referenceBase: 'https://vocabulary.simpl.local/vocabularies',
}

/** Loads /config.json at start-up, so the image can be configured without a rebuild. */
export async function loadConfig(): Promise<AppConfig> {
  try {
    const res = await fetch('/config.json', { cache: 'no-store' })
    if (res.ok) config = { ...config, ...(await res.json()) }
  } catch {
    // keep defaults
  }
  return config
}

export function getConfig(): AppConfig {
  return config
}
