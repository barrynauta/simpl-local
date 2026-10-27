import { accessToken, login } from './auth'
import { getConfig } from './config'
import type {
  Page, Problem, SearchItem, Status, VersionCreated, VocabularyCategory, VocabularyMetadata,
} from './types'

/** An error response from the API, with the Problem Details body when there is one. */
export class ApiError extends Error {
  constructor(public status: number, public problem: Problem | null, message: string) {
    super(message)
  }
}

async function call(path: string, init: RequestInit = {}): Promise<Response> {
  const token = accessToken()
  if (!token) {
    await login()
    throw new ApiError(401, null, 'Authentication required')
  }
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${token}`)
  let res: Response
  try {
    res = await fetch(getConfig().apiBase + path, { ...init, headers })
  } catch (e) {
    throw new ApiError(0, null, `The server could not be reached (${(e as Error).message}).`)
  }
  if (!res.ok) {
    let problem: Problem | null = null
    try {
      const text = await res.text()
      problem = text ? (JSON.parse(text) as Problem) : null
    } catch {
      problem = null
    }
    if (res.status === 401) {
      await login()
    }
    throw new ApiError(res.status, problem, problem?.detail ?? problem?.title ?? `HTTP ${res.status}`)
  }
  return res
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  const res = await call(path, { ...init, headers })
  return (await res.json()) as T
}

export interface ListQuery {
  name?: string
  status?: Status | ''
  creationDate?: string
  lastUpdated?: string
  sort?: string
  page?: number
  pageSize?: number
}

function qs(params: Record<string, string | number | undefined>): string {
  const p = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') p.set(k, String(v))
  }
  const s = p.toString()
  return s ? `?${s}` : ''
}

export const api = {
  list: (q: ListQuery) =>
    json<Page<VocabularyCategory>>(`/vocabularies${qs({ ...q } as Record<string, string | number | undefined>)}`),
  get: (name: string) => json<VocabularyCategory>(`/vocabularies/${encodeURIComponent(name)}`),
  versions: (name: string, page = 1, pageSize = 100) =>
    json<Page<VocabularyMetadata>>(`/vocabularies/${encodeURIComponent(name)}/versions${qs({ page, pageSize })}`),
  version: (name: string, version: number) =>
    json<VocabularyMetadata>(`/vocabularies/${encodeURIComponent(name)}/versions/${version}`),
  search: (items: { name: string; version?: number }[]) =>
    json<Page<SearchItem>>('/vocabularies/searches', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items }),
    }),
  create: (name: string, description: string, file: Blob, filename: string) => {
    const form = new FormData()
    form.append('name', name)
    form.append('description', description)
    form.append('changelog', 'Initial version') // D12: no change log field on the creation form
    form.append('vocabularyFile', file, filename)
    return json<VersionCreated>('/vocabularies', { method: 'POST', body: form })
  },
  createVersion: (name: string, description: string, changelog: string, file: Blob, filename: string) => {
    const form = new FormData()
    form.append('description', description)
    form.append('changelog', changelog)
    form.append('vocabularyFile', file, filename)
    return json<VersionCreated>(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form })
  },
  setStatus: (name: string, status: Status) =>
    json<VocabularyCategory>(`/vocabularies/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    }),
  contentText: async (name: string, version?: number) => {
    const path = version ? `/content/${encodeURIComponent(name)}/versions/${version}` : `/content/${encodeURIComponent(name)}`
    const res = await call(path, { headers: { Accept: 'text/turtle' } })
    return res.text()
  },
  /** Downloads the Turtle file of the latest version or of one version (D24). */
  download: async (name: string, version?: number) => {
    const path = version ? `/content/${encodeURIComponent(name)}/versions/${version}` : `/content/${encodeURIComponent(name)}`
    const res = await call(path, { headers: { Accept: 'text/turtle' } })
    const blob = await res.blob()
    const disposition = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^";]+)"?/.exec(disposition)
    const filename = match ? match[1] : version ? `${name}-v${version}.ttl` : `${name}.ttl`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    return filename
  },
}
