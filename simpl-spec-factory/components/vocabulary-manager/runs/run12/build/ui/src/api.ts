import { getToken } from './auth';

export type VocabularyStatus = 'ACTIVE' | 'DEPRECATED';

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description?: string;
  status: VocabularyStatus;
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
  description?: string;
  changelog?: string;
  status: VocabularyStatus;
  createdBy?: string;
  creationDate: string;
  lastUpdateDate: string;
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

export interface ProblemIssue {
  type?: string;
  title?: string;
  detail?: string;
  in?: string;
  name?: string;
  location?: string;
}

export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  issues?: ProblemIssue[];
  truncated?: boolean;
}

/** An error answer of the API, carrying the problem+json body when there is one. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problem | null;

  constructor(status: number, problem: Problem | null, message?: string) {
    super(message ?? problem?.detail ?? `Request failed with status ${status}.`);
    this.status = status;
    this.problem = problem;
  }
}

export const API_BASE = '/v1';

function correlationId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

async function request(path: string, init: RequestInit = {}, accept = 'application/json') {
  const token = await getToken();
  const headers = new Headers(init.headers);
  headers.set('Accept', accept);
  headers.set('X-Correlation-Id', correlationId());
  if (token) headers.set('Authorization', `Bearer ${token}`);
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(0, null, 'The server could not be reached.');
  }
  if (!res.ok) {
    let problem: Problem | null;
    try {
      problem = (await res.json()) as Problem;
    } catch {
      problem = null;
    }
    throw new ApiError(res.status, problem);
  }
  return res;
}

function enc(value: string): string {
  return encodeURIComponent(value);
}

export interface ListParams {
  page: number;
  pageSize: number;
  sort?: string;
  name?: string;
  status?: string;
  creationDate?: string;
}

export function filenameFromDisposition(header: string | null, fallback: string): string {
  if (!header) return fallback;
  const match = /filename="?([^";]+)"?/i.exec(header);
  return match ? match[1] : fallback;
}

export const api = {
  async listVocabularies(params: ListParams): Promise<Page<Vocabulary>> {
    const query = new URLSearchParams();
    query.set('page', String(params.page));
    query.set('pageSize', String(params.pageSize));
    if (params.sort) query.set('sort', params.sort);
    if (params.name) query.set('name', params.name);
    if (params.status) query.set('status', params.status);
    if (params.creationDate) query.set('creationDate', params.creationDate);
    const res = await request(`/vocabularies?${query.toString()}`);
    return (await res.json()) as Page<Vocabulary>;
  },

  async getVocabulary(name: string): Promise<Vocabulary> {
    const res = await request(`/vocabularies/${enc(name)}`);
    return (await res.json()) as Vocabulary;
  },

  /** D35: loads every version with pageSize 100, page after page. */
  async listAllVersions(name: string): Promise<VocabularyVersion[]> {
    const all: VocabularyVersion[] = [];
    let page = 1;
    for (;;) {
      const res = await request(`/vocabularies/${enc(name)}/versions?page=${page}&pageSize=100`);
      const body = (await res.json()) as Page<VocabularyVersion>;
      all.push(...body.items);
      if (body.items.length === 0 || all.length >= body.total || page >= body.totalPages) break;
      page += 1;
    }
    return all;
  },

  async changeStatus(name: string, status: VocabularyStatus): Promise<Vocabulary> {
    const res = await request(`/vocabularies/${enc(name)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    return (await res.json()) as Vocabulary;
  },

  async createVocabulary(form: FormData): Promise<CreatedVersion> {
    const res = await request('/vocabularies', { method: 'POST', body: form });
    return (await res.json()) as CreatedVersion;
  },

  async createVersion(name: string, form: FormData): Promise<CreatedVersion> {
    const res = await request(`/vocabularies/${enc(name)}/versions`, {
      method: 'POST',
      body: form,
    });
    return (await res.json()) as CreatedVersion;
  },

  /** D29: the stored Turtle bytes, latest version or one version. */
  async downloadContent(name: string, version?: number): Promise<{ blob: Blob; filename: string }> {
    const path =
      version === undefined ? `/content/${enc(name)}` : `/content/${enc(name)}/versions/${version}`;
    const res = await request(path, {}, 'text/turtle');
    const fallback = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
    return {
      blob: await res.blob(),
      filename: filenameFromDisposition(res.headers.get('Content-Disposition'), fallback),
    };
  },
};

/** Hands a downloaded file to the browser. */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
