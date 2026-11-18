import { accessToken } from './auth';
import type {
  CollectionPage,
  CreatedVersion,
  Problem,
  Vocabulary,
  VocabularyStatus,
  VocabularyVersion,
} from './types';

let apiBase = '/v1';

export function setApiBase(base: string): void {
  apiBase = base;
}

/** An API call that did not succeed, with the problem body when there is one. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problem;

  constructor(status: number, problem: Problem) {
    super(problem.detail ?? `Request failed with status ${status}.`);
    this.status = status;
    this.problem = problem;
  }
}

async function request(path: string, init: RequestInit & { accept?: string } = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${await accessToken()}`);
  headers.set('Accept', init.accept ?? 'application/json');
  let response: Response;
  try {
    response = await fetch(apiBase + path, { ...init, headers });
  } catch {
    throw new ApiError(0, { status: 0, detail: 'The service could not be reached.' });
  }
  if (!response.ok) {
    let problem: Problem = { status: response.status, detail: response.statusText };
    try {
      problem = { ...((await response.json()) as Problem), status: response.status };
    } catch {
      // Not a problem body; keep the status text.
    }
    throw new ApiError(response.status, problem);
  }
  return response;
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  return (await (await request(path, init)).json()) as T;
}

export interface ListQuery {
  name?: string;
  status?: VocabularyStatus | '';
  creationDate?: string;
  lastUpdated?: string;
  sort?: string;
  page: number;
  pageSize: number;
}

export function listVocabularies(query: ListQuery): Promise<CollectionPage<Vocabulary>> {
  const params = new URLSearchParams();
  if (query.name) params.set('name', query.name);
  if (query.status) params.set('status', query.status);
  if (query.creationDate) params.set('creationDate', query.creationDate);
  if (query.lastUpdated) params.set('lastUpdated', query.lastUpdated);
  if (query.sort) params.set('sort', query.sort);
  params.set('page', String(query.page));
  params.set('pageSize', String(query.pageSize));
  return json(`/vocabularies?${params.toString()}`);
}

export function getVocabulary(name: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`);
}

/** All versions, newest first, in pages of 100 (decision D35). */
export async function listAllVersions(name: string): Promise<VocabularyVersion[]> {
  const versions: VocabularyVersion[] = [];
  let page = 1;
  for (;;) {
    const result = await json<CollectionPage<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
    );
    versions.push(...result.items);
    if (page >= result.totalPages) return versions;
    page += 1;
  }
}

export function updateVocabularyStatus(name: string, status: VocabularyStatus): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
}

export function createVocabulary(name: string, description: string, file: Blob): Promise<CreatedVersion> {
  const form = new FormData();
  form.append('name', name);
  form.append('description', description);
  form.append('changelog', 'Initial version.');
  form.append('vocabularyFile', file, `${name}.ttl`);
  return json('/vocabularies', { method: 'POST', body: form });
}

export function createVersion(
  name: string,
  description: string,
  changelog: string,
  file: Blob,
): Promise<CreatedVersion> {
  const form = new FormData();
  form.append('description', description);
  form.append('changelog', changelog);
  form.append('vocabularyFile', file, `${name}.ttl`);
  return json(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form });
}

/** The stored Turtle of the latest version, or of one version. */
export async function fetchContent(
  name: string,
  version?: number,
): Promise<{ blob: Blob; filename: string }> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`;
  const response = await request(path, { accept: 'text/turtle' });
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const match = /filename="?([^";]+)"?/.exec(disposition);
  const filename = match ? match[1] : version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  return { blob: await response.blob(), filename };
}

/** Downloads the content as a file in the browser. */
export async function download(name: string, version?: number): Promise<void> {
  const { blob, filename } = await fetchContent(name, version);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
