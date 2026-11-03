import { currentUser } from './auth'

const BASE = '/contract-ui/v1'

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
  }
}

async function call<T>(method: string, path: string, body?: unknown): Promise<T> {
  const user = await currentUser()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (user) headers.Authorization = `Bearer ${user.access_token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  const text = await res.text()
  const json = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new ApiError(res.status, json?.detail ?? json?.title ?? `Request failed with status ${res.status}`)
  }
  return json as T
}

export interface ContractItem {
  contractNegotiationId: string
  resourceDescriptionId: string | null
  resourceName: string | null
  providerName: string | null
  consumerName: string | null
  partyRole: string
  status: string
  consentStatus: string | null
}

export interface HistoryItem {
  at: string
  fromStatus: string | null
  toStatus: string | null
  entryType: string
  trigger: string
  actor: string | null
  reasonCode: string | null
  reason: string | null
}

export interface ContractDetails extends ContractItem {
  allowedActions: string[]
  history: HistoryItem[]
  humanReadable: string | null
  humanReadableData: string | null
  consentManagerUrl?: string
  reasonCode: string | null
  revocationRequestedBy: string | null
}

export function listContracts(filters: Record<string, string>): Promise<{ items: ContractItem[] }> {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(filters)) if (v) q.set(k, v)
  const qs = q.toString()
  return call('GET', `/contracts${qs ? `?${qs}` : ''}`)
}

export function getContract(nid: string): Promise<ContractDetails> {
  return call('GET', `/contracts/${encodeURIComponent(nid)}`)
}

export function decideOffer(nid: string, op: 'accept' | 'reject'): Promise<unknown> {
  return call('PATCH', `/contractOffers/${encodeURIComponent(nid)}/${op}`)
}

export function lifecycleAction(nid: string, action: string, reason?: string): Promise<ContractDetails> {
  return call('POST', `/contracts/${encodeURIComponent(nid)}/lifecycleActions`, { action, reason: reason ?? null })
}
