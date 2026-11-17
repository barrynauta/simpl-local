import { API_BASE } from './config'
import { token } from './auth'

export interface HistoryEntry {
  at: string
  fromStatus: string | null
  toStatus: string | null
  entryType: string
  trigger: string
  actor: string | null
  reasonCode: string | null
  reason: string | null
}

export interface ContractItem {
  contractNegotiationId: string
  contractAgreementId: string | null
  resourceDescriptionId: string | null
  resourceName: string | null
  providerName: string | null
  consumerName: string | null
  partyRole: string
  status: string
  consentStatus: string
}

export interface ContractDetails extends ContractItem {
  allowedActions: string[]
  history: HistoryEntry[]
  humanReadable: string | null
  humanReadableData: string | null
  consentManagerUrl?: string
  reasonCode: string | null
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${await token()}`,
    Accept: 'application/json'
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  const json = text ? JSON.parse(text) : null
  if (!res.ok) {
    const detail = json && typeof json.detail === 'string' ? json.detail : `Request failed with status ${res.status}`
    throw new ApiError(res.status, detail)
  }
  return json as T
}

export function listContracts(filters: Record<string, string>): Promise<{ items: ContractItem[] }> {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(filters)) {
    if (v) q.set(k, v)
  }
  const qs = q.toString()
  return request('GET', '/contracts' + (qs ? `?${qs}` : ''))
}

export function getContract(nid: string): Promise<ContractDetails> {
  return request('GET', `/contracts/${encodeURIComponent(nid)}`)
}

export function decideOffer(nid: string, op: 'accept' | 'reject'): Promise<unknown> {
  return request('PATCH', `/contractOffers/${encodeURIComponent(nid)}/${op}`)
}

export function lifecycleAction(nid: string, action: string, reason?: string): Promise<ContractDetails> {
  return request('POST', `/contracts/${encodeURIComponent(nid)}/lifecycleActions`, { action, reason })
}
