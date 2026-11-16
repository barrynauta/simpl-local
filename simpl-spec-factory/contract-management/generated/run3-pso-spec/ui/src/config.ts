export const config = window.__CONFIG__ ?? {
  keycloakUrl: 'http://localhost:19080',
  realm: 'provider',
  clientId: 'contract-ui',
  partyRole: 'PROVIDER' as const,
  partyName: 'Provider One'
}

export const API_BASE = '/contract-ui/v1'
