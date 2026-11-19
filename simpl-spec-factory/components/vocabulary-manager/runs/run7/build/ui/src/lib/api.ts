import { token } from './auth'

/** RFC 9457 problem body. */
export interface ProblemIssue {
  type?: string
  detail?: string
  in?: string
  name?: string
  location?: string
}
export interface Problem {
  type?: string
  title?: string
  status?: number
  detail?: string
  issues?: ProblemIssue[]
}

export interface Vocabulary {
  href: string
  contentHref: string
  name: string
  version: number
  description: string
  status: 'ACTIVE' | 'DEPRECATED'
  createdBy?: string
  creationDate: string
  lastUpdateDate: string
}

export interface VocabularyVersion {
  href: string
  contentHref: string
  latestVersionHref?: string
  name: string
  version: number
  description: string
  changelog: string
  status: 'ACTIVE' | 'DEPRECATED'
  createdBy?: string
  creationDate: string
  lastUpdateDate: string
}

export interface ValidationWarning {
  code: string
  description: string
  location: string
}

export interface CreatedVersion extends VocabularyVersion {
  validationWarnings: ValidationWarning[]
}

export interface PageOf<T> {
  page: number
  totalPages: number
  total: number
  pageSize: number
  items: T[]
}

/** An API call that did not succeed. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Problem,
  ) {
    super(problem.detail ?? `Request failed with status ${status}`)
  }
}

const BASE = '/v1'

async function call(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${await token()}`)
  let response: Response
  try {
    response = await fetch(BASE + path, { ...init, headers })
  } catch {
    throw new ApiError(0, { detail: 'The server could not be reached.' })
  }
  if (!response.ok) {
    let problem: Problem = { status: response.status }
    try {
      problem = (await response.json()) as Problem
    } catch {
      problem = { status: response.status, detail: response.statusText }
    }
    throw new ApiError(response.status, problem)
  }
  return response
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  const response = await call(path, { ...init, headers })
  return (await response.json()) as T
}

export interface ListQuery {
  name?: string
  status?: string
  creationDate?: string
  sort?: string
  page: number
  pageSize: number
}

export function listVocabularies(query: ListQuery): Promise<PageOf<Vocabulary>> {
  const params = new URLSearchParams()
  if (query.name) params.set('name', query.name)
  if (query.status) params.set('status', query.status)
  if (query.creationDate) params.set('creationDate', query.creationDate)
  if (query.sort) params.set('sort', query.sort)
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  return json(`/vocabularies?${params.toString()}`)
}

export function getVocabulary(name: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`)
}

/** Loads every version, 100 per page (D35). */
export async function listAllVersions(name: string): Promise<VocabularyVersion[]> {
  const all: VocabularyVersion[] = []
  let page = 1
  for (;;) {
    const result = await json<PageOf<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
    )
    all.push(...result.items)
    if (page >= result.totalPages || result.items.length === 0) {
      return all
    }
    page++
  }
}

export function updateVocabularyStatus(name: string, status: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
}

export function createVocabulary(form: FormData): Promise<CreatedVersion> {
  return json('/vocabularies', { method: 'POST', body: form })
}

export function createVersion(name: string, form: FormData): Promise<CreatedVersion> {
  return json(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form })
}

/** Downloads Turtle content (D29). */
export async function fetchContent(name: string, version?: number): Promise<Blob> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`
  const response = await call(path, { headers: { Accept: 'text/turtle' } })
  return await response.blob()
}

/** Saves the content as a file with the decided name. */
export async function downloadContent(name: string, version?: number): Promise<void> {
  const blob = await fetchContent(name, version)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
