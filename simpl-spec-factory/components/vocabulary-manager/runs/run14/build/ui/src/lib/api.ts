import { accessToken } from './auth';
import { config } from './config';

export interface Issue {
  type?: string;
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
  issues?: Issue[];
  truncated?: boolean;
}

export interface ValidationWarning {
  code: string;
  description: string;
  location: string;
}

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: 'ACTIVE' | 'DEPRECATED';
  createdBy?: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface VocabularyVersion {
  href: string;
  contentHref: string;
  latestVersionHref?: string;
  name: string;
  version: number;
  description: string;
  changelog: string;
  status: 'ACTIVE' | 'DEPRECATED';
  createdBy?: string;
  creationDate: string;
  lastUpdateDate: string;
  validationWarnings?: ValidationWarning[];
}

export interface ExternalVocabulary {
  href: string;
  name: string;
  namespaceUri: string;
  aliasUris: string[];
  sourceUri?: string;
  description?: string;
  createdBy?: string;
  creationDate: string;
}

export interface Page<T> {
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
  items: T[];
}

/** An error answer of the API, with its problem body. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: Problem,
  ) {
    super(problem.detail ?? `HTTP ${status}`);
  }
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const token = await accessToken();
  const headers = new Headers(init.headers);
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  let response: Response;
  try {
    response = await fetch(config().apiBase + path, { ...init, headers });
  } catch {
    throw new ApiError(503, { status: 503, detail: 'The service could not be reached.' });
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

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await request(path, init);
  return (await response.json()) as T;
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  }
  const text = search.toString().replace(/\+/g, '%20');
  return text ? `?${text}` : '';
}

export const api = {
  listVocabularies(params: Record<string, string | number | undefined>): Promise<Page<Vocabulary>> {
    return json(`/vocabularies${query(params)}`);
  },
  getVocabulary(name: string): Promise<Vocabulary> {
    return json(`/vocabularies/${encodeURIComponent(name)}`);
  },
  listVersions(name: string, page: number): Promise<Page<VocabularyVersion>> {
    return json(`/vocabularies/${encodeURIComponent(name)}/versions${query({ page, pageSize: 100 })}`);
  },
  createVocabulary(form: FormData): Promise<VocabularyVersion> {
    return json('/vocabularies', { method: 'POST', body: form });
  },
  createVersion(name: string, form: FormData): Promise<VocabularyVersion> {
    return json(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form });
  },
  changeStatus(name: string, status: string): Promise<Vocabulary> {
    return json(`/vocabularies/${encodeURIComponent(name)}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
  },
  async content(name: string, version?: number): Promise<Blob> {
    const path =
      version === undefined
        ? `/content/${encodeURIComponent(name)}`
        : `/content/${encodeURIComponent(name)}/versions/${version}`;
    const response = await request(path, { headers: { Accept: 'text/turtle' } });
    return response.blob();
  },
  listExternal(page: number): Promise<Page<ExternalVocabulary>> {
    return json(`/external-vocabularies${query({ page, pageSize: 20 })}`);
  },
  registerExternal(form: FormData): Promise<ExternalVocabulary> {
    return json('/external-vocabularies', { method: 'POST', body: form });
  },
  async removeExternal(name: string): Promise<void> {
    await request(`/external-vocabularies/${encodeURIComponent(name)}`, { method: 'DELETE' });
  },
  async externalContent(name: string): Promise<Blob> {
    const response = await request(`/external-vocabularies/${encodeURIComponent(name)}/content`, {
      headers: { Accept: 'text/turtle' },
    });
    return response.blob();
  },
};

/** Offers a blob as a file download. */
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
