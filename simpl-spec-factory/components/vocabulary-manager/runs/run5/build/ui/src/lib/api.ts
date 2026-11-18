import { token } from './auth';

/** A problem response of the API (RFC 9457). */
export interface ProblemIssue {
  detail?: string;
  location?: string;
  in?: string;
  name?: string;
}

export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  issues?: ProblemIssue[];
}

export class ApiError extends Error {
  constructor(public readonly problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
  }

  get status(): number {
    return this.problem.status;
  }
}

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description?: string;
  status: string;
  createdBy?: string;
  creationDate?: string;
  lastUpdateDate?: string;
}

export interface VocabularyVersion {
  href: string;
  name: string;
  version: number;
  description?: string;
  changelog?: string;
  status: string;
  createdBy?: string;
  creationDate?: string;
  lastUpdateDate?: string;
}

export interface ValidationWarning {
  code: string;
  description: string;
  location?: string;
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

const API_BASE = '/v1';

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  const bearer = await token();
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`);
  }
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }
  let response: Response;
  try {
    response = await fetch(API_BASE + path, { ...init, headers });
  } catch {
    throw new ApiError({ status: 0, detail: 'The server could not be reached.' });
  }
  if (!response.ok) {
    let problem: Problem = { status: response.status };
    try {
      problem = { ...((await response.json()) as Problem), status: response.status };
    } catch {
      problem = { status: response.status, detail: response.statusText };
    }
    throw new ApiError(problem);
  }
  return response;
}

export async function getJson<T>(path: string): Promise<T> {
  return (await (await request(path)).json()) as T;
}

export async function patchStatus<T>(path: string, status: string): Promise<T> {
  const response = await request(path, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  return (await response.json()) as T;
}

export async function postForm<T>(path: string, form: FormData): Promise<T> {
  const response = await request(path, { method: 'POST', body: form });
  return (await response.json()) as T;
}

export async function getBlob(path: string): Promise<{ blob: Blob; disposition: string | null }> {
  const response = await request(path, { headers: { Accept: 'text/turtle' } });
  return { blob: await response.blob(), disposition: response.headers.get('Content-Disposition') };
}

export function listVocabularies(query: URLSearchParams): Promise<Page<Vocabulary>> {
  return getJson<Page<Vocabulary>>(`/vocabularies?${query.toString()}`);
}

export function getVocabulary(name: string): Promise<Vocabulary> {
  return getJson<Vocabulary>(`/vocabularies/${encodeURIComponent(name)}`);
}

/** All versions, newest first, pageSize 100 with further pages when total is higher (D35). */
export async function getAllVersions(name: string): Promise<VocabularyVersion[]> {
  const all: VocabularyVersion[] = [];
  let page = 1;
  for (;;) {
    const result = await getJson<Page<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
    );
    all.push(...result.items);
    if (page >= result.totalPages || result.items.length === 0) {
      return all;
    }
    page++;
  }
}

export function setVocabularyStatus(name: string, status: string): Promise<Vocabulary> {
  return patchStatus<Vocabulary>(`/vocabularies/${encodeURIComponent(name)}`, status);
}

export function createVocabulary(form: FormData): Promise<CreatedVersion> {
  return postForm<CreatedVersion>('/vocabularies', form);
}

export function createVersion(name: string, form: FormData): Promise<CreatedVersion> {
  return postForm<CreatedVersion>(`/vocabularies/${encodeURIComponent(name)}/versions`, form);
}

export async function fetchContent(
  name: string,
  version?: number,
): Promise<{ blob: Blob; fileName: string }> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`;
  const { blob, disposition } = await getBlob(path);
  const fallback = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  const match = disposition ? /filename="?([^";]+)"?/i.exec(disposition) : null;
  return { blob, fileName: match ? match[1] : fallback };
}

/** Starts a browser download of a version's content. */
export async function download(name: string, version?: number): Promise<void> {
  const { blob, fileName } = await fetchContent(name, version);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
