import { session } from './auth';

/** Wire types of the Vocabulary Management API (docs/openapi-decided.yaml). */
export type Status = 'ACTIVE' | 'DEPRECATED';

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: Status;
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface VocabularyVersion {
  href: string;
  name: string;
  version: number;
  description: string;
  changelog: string;
  status: Status;
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
  latestVersionHref: string;
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
  totalPages: number;
  total: number;
  pageSize: number;
  items: T[];
}

export interface ProblemIssue {
  detail?: string;
  location?: string;
  name?: string;
}

export interface Problem {
  status?: number;
  title?: string;
  detail?: string;
  issues?: ProblemIssue[];
}

/** A failed API call with its HTTP status and RFC 9457 problem body. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Problem,
  ) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`);
  }
}

const BASE = '/v1';

async function call(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${await session().token()}`);
  let response: Response;
  try {
    response = await fetch(BASE + path, { ...init, headers });
  } catch {
    throw new ApiError(0, { detail: 'The service could not be reached.' });
  }
  if (!response.ok) {
    let problem: Problem;
    try {
      problem = (await response.json()) as Problem;
    } catch {
      problem = { status: response.status, detail: response.statusText };
    }
    throw new ApiError(response.status, problem);
  }
  return response;
}

export interface ListQuery {
  name?: string;
  status?: Status | '';
  creationDate?: string;
  sort: string;
  page: number;
  pageSize: number;
}

export async function listVocabularies(query: ListQuery): Promise<Page<Vocabulary>> {
  const params = new URLSearchParams();
  if (query.name) params.set('name', query.name);
  if (query.status) params.set('status', query.status);
  if (query.creationDate) params.set('creationDate', query.creationDate);
  params.set('sort', query.sort);
  params.set('page', String(query.page));
  params.set('pageSize', String(query.pageSize));
  return (await call(`/vocabularies?${params.toString()}`)).json();
}

export async function getVocabulary(name: string): Promise<Vocabulary> {
  return (await call(`/vocabularies/${encodeURIComponent(name)}`)).json();
}

/** All versions, newest first, with pageSize 100 and further pages when total is higher (D35). */
export async function listAllVersions(name: string): Promise<VocabularyVersion[]> {
  const versions: VocabularyVersion[] = [];
  for (let page = 1; ; page++) {
    const result: Page<VocabularyVersion> = await (
      await call(`/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`)
    ).json();
    versions.push(...result.items);
    if (page >= result.totalPages) {
      return versions;
    }
  }
}

export async function changeStatus(name: string, status: Status): Promise<Vocabulary> {
  return (
    await call(`/vocabularies/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    })
  ).json();
}

export async function createVocabulary(
  name: string,
  description: string,
  file: Blob,
  fileName: string,
): Promise<CreatedVersion> {
  const form = new FormData();
  form.append('name', name);
  form.append('description', description);
  form.append('changelog', 'Initial version.');
  form.append('vocabularyFile', file, fileName);
  return (await call('/vocabularies', { method: 'POST', body: form })).json();
}

export async function createVersion(
  name: string,
  description: string,
  changelog: string,
  file: Blob,
  fileName: string,
): Promise<CreatedVersion> {
  const form = new FormData();
  form.append('description', description);
  form.append('changelog', changelog);
  form.append('vocabularyFile', file, fileName);
  return (
    await call(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form })
  ).json();
}

/** The latest content (D29), used to pre-fill the new version form (D33). */
export async function latestContent(name: string): Promise<Blob> {
  return (await call(`/content/${encodeURIComponent(name)}`)).blob();
}

/** Downloads the latest content or one version and offers it as a file (D29). */
export async function download(name: string, version?: number): Promise<void> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`;
  const blob = await (await call(path)).blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
