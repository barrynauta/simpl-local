import { getSession } from './auth';

export type Status = 'ACTIVE' | 'DEPRECATED';

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: Status;
  createdBy?: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface VocabularyVersion {
  href: string;
  contentHref: string;
  latestVersionHref?: string | null;
  name: string;
  version: number;
  description: string;
  changelog: string;
  status: Status;
  createdBy?: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface ValidationWarning {
  code: string;
  description: string;
  location: string;
}

export interface CreatedVersion extends VocabularyVersion {
  validationWarnings: ValidationWarning[];
}

export interface Page<T> {
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
  items: T[];
}

export interface ProblemIssue {
  detail?: string;
  location?: string;
  in?: string;
  name?: string;
}

export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  issues?: ProblemIssue[];
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: Problem,
  ) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`);
  }
}

const API_BASE = '/v1';

async function authHeaders(accept: string): Promise<Record<string, string>> {
  const headers: Record<string, string> = { Accept: accept };
  const token = await getSession()?.token();
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function readProblem(response: Response): Promise<Problem> {
  try {
    const text = await response.text();
    return text ? (JSON.parse(text) as Problem) : { status: response.status };
  } catch {
    return { status: response.status, detail: response.statusText };
  }
}

async function send(path: string, init: RequestInit, accept: string): Promise<Response> {
  const headers = { ...(await authHeaders(accept)), ...((init.headers as object) ?? {}) };
  let response: Response;
  try {
    response = await fetch(API_BASE + path, { ...init, headers });
  } catch {
    throw new ApiError(503, { status: 503, detail: 'The service could not be reached.' });
  }
  if (!response.ok) {
    const problem = await readProblem(response);
    if (response.status === 401) getSession()?.login();
    throw new ApiError(response.status, problem);
  }
  return response;
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await send(path, init, 'application/json');
  return (await response.json()) as T;
}

export interface ListQuery {
  name?: string;
  status?: Status | '';
  creationDate?: string;
  sort?: string;
  page?: number;
  pageSize?: number;
}

export function buildQuery(query: ListQuery): string {
  const params = new URLSearchParams();
  if (query.name) params.set('name', query.name);
  if (query.status) params.set('status', query.status);
  if (query.creationDate) params.set('creationDate', query.creationDate);
  if (query.sort) params.set('sort', query.sort);
  if (query.page) params.set('page', String(query.page));
  if (query.pageSize) params.set('pageSize', String(query.pageSize));
  const text = params.toString();
  return text ? `?${text}` : '';
}

export function listVocabularies(query: ListQuery): Promise<Page<Vocabulary>> {
  return json<Page<Vocabulary>>(`/vocabularies${buildQuery(query)}`);
}

export function getVocabulary(name: string): Promise<Vocabulary> {
  return json<Vocabulary>(`/vocabularies/${encodeURIComponent(name)}`);
}

/** Loads every version, newest first, with pageSize 100 and further pages when needed (D35). */
export async function listAllVersions(name: string): Promise<VocabularyVersion[]> {
  const result: VocabularyVersion[] = [];
  let page = 1;
  for (;;) {
    const data = await json<Page<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
    );
    result.push(...data.items);
    if (page >= data.totalPages || data.items.length === 0) return result;
    page += 1;
  }
}

export function createVocabulary(form: FormData): Promise<CreatedVersion> {
  return json<CreatedVersion>('/vocabularies', { method: 'POST', body: form });
}

export function createVersion(name: string, form: FormData): Promise<CreatedVersion> {
  return json<CreatedVersion>(`/vocabularies/${encodeURIComponent(name)}/versions`, {
    method: 'POST',
    body: form,
  });
}

export function patchVocabularyStatus(name: string, status: Status): Promise<Vocabulary> {
  return json<Vocabulary>(`/vocabularies/${encodeURIComponent(name)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
}

function contentPath(name: string, version?: number): string {
  const base = `/content/${encodeURIComponent(name)}`;
  return version === undefined ? base : `${base}/versions/${version}`;
}

export function filenameFrom(disposition: string | null, fallback: string): string {
  const match = disposition ? /filename="?([^";]+)"?/i.exec(disposition) : null;
  return match ? match[1] : fallback;
}

export async function fetchContent(
  name: string,
  version?: number,
): Promise<{ blob: Blob; filename: string }> {
  const response = await send(contentPath(name, version), {}, 'text/turtle');
  const fallback = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  return {
    blob: await response.blob(),
    filename: filenameFrom(response.headers.get('Content-Disposition'), fallback),
  };
}

export async function downloadContent(name: string, version?: number): Promise<void> {
  const { blob, filename } = await fetchContent(name, version);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
