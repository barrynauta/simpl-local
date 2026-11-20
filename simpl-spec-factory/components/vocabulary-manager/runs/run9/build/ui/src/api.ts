import { session } from './auth';

/** An RFC 9457 problem as returned by the API. */
export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  issues?: { detail?: string; location?: string; name?: string; in?: string }[];
}

/** Thrown for every non-2xx answer. */
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
  description: string;
  status: 'ACTIVE' | 'DEPRECATED';
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
  status: 'ACTIVE' | 'DEPRECATED';
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
  latestVersionHref?: string;
}

export interface ValidationWarning {
  code: string;
  description: string;
  location: string;
}

export interface CreatedVersion extends VocabularyVersion {
  validationWarnings: ValidationWarning[];
}

export interface CollectionPage<T> {
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
  items: T[];
}

export interface ListQuery {
  name?: string;
  status?: string;
  creationDate?: string;
  sort?: string;
  page?: number;
  pageSize?: number;
}

const BASE = '/v1';

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const token = await session().token();
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(BASE + path, { ...init, headers });
  if (!response.ok) {
    let problem: Problem = { status: response.status };
    try {
      problem = { ...(await response.json()), status: response.status };
    } catch {
      // Not a problem body; keep the status only.
    }
    throw new ApiError(problem);
  }
  return response;
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await request(path, { ...init, headers: { Accept: 'application/json', ...init.headers } });
  return (await response.json()) as T;
}

/** The API calls the UI uses; the same public API as any client (MACH-04). */
export const api = {
  listVocabularies(query: ListQuery): Promise<CollectionPage<Vocabulary>> {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        params.set(key, String(value));
      }
    });
    return json(`/vocabularies?${params.toString()}`);
  },
  getVocabulary(name: string): Promise<Vocabulary> {
    return json(`/vocabularies/${encodeURIComponent(name)}`);
  },
  async listAllVersions(name: string): Promise<VocabularyVersion[]> {
    const all: VocabularyVersion[] = [];
    let page = 1;
    for (;;) {
      const result = await json<CollectionPage<VocabularyVersion>>(
        `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
      );
      all.push(...result.items);
      if (page >= result.totalPages || result.items.length === 0) {
        return all;
      }
      page += 1;
    }
  },
  changeStatus(name: string, status: string): Promise<Vocabulary> {
    return json(`/vocabularies/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
  },
  async content(name: string, version?: number): Promise<Blob> {
    const path = version
      ? `/content/${encodeURIComponent(name)}/versions/${version}`
      : `/content/${encodeURIComponent(name)}`;
    const response = await request(path, { headers: { Accept: 'text/turtle' } });
    return response.blob();
  },
  createVocabulary(form: FormData): Promise<CreatedVersion> {
    return json('/vocabularies', { method: 'POST', body: form });
  },
  createVersion(name: string, form: FormData): Promise<CreatedVersion> {
    return json(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form });
  },
};

/** Saves a downloaded file in the browser. */
export function saveFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
