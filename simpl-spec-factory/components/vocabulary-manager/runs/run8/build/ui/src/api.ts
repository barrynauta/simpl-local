/** Client of the public Vocabulary Management API (MACH-04: the UI uses the same API as any client). */

export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  issues?: { detail?: string; location?: string; name?: string; in?: string }[];
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
  description: string;
  status: 'ACTIVE' | 'DEPRECATED';
  createdBy: string;
  creationDate: string;
  lastUpdateDate: string;
}

export interface VocabularyVersion {
  href: string;
  contentHref: string;
  latestVersionHref: string;
  name: string;
  version: number;
  description: string;
  changelog: string;
  status: 'ACTIVE' | 'DEPRECATED';
  createdBy: string;
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

export interface CollectionPage<T> {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
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

export type TokenSource = () => Promise<string>;

export class VocabularyApi {
  constructor(
    private readonly base: string,
    private readonly token: TokenSource,
    private readonly fetcher: typeof fetch = (input, init) => fetch(input, init),
  ) {}

  private async request(path: string, init: RequestInit = {}, accept = 'application/json'): Promise<Response> {
    const headers = new Headers(init.headers);
    headers.set('Authorization', `Bearer ${await this.token()}`);
    headers.set('Accept', accept);
    let response: Response;
    try {
      response = await this.fetcher(`${this.base}${path}`, { ...init, headers });
    } catch {
      throw new ApiError({ status: 0, detail: 'The server could not be reached.' });
    }
    if (!response.ok) {
      let problem: Problem = { status: response.status };
      try {
        problem = { ...(await response.json()), status: response.status };
      } catch {
        // Body is not a problem document; keep the status only.
      }
      throw new ApiError(problem);
    }
    return response;
  }

  async listVocabularies(query: ListQuery): Promise<CollectionPage<Vocabulary>> {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        params.set(key, String(value));
      }
    }
    const text = params.toString();
    return (await this.request(`/vocabularies${text ? `?${text}` : ''}`)).json();
  }

  async getVocabulary(name: string): Promise<Vocabulary> {
    return (await this.request(`/vocabularies/${encodeURIComponent(name)}`)).json();
  }

  async listVersions(name: string, page: number, pageSize = 100): Promise<CollectionPage<VocabularyVersion>> {
    return (
      await this.request(`/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=${pageSize}`)
    ).json();
  }

  /** All versions, newest first: pageSize 100 and further pages when total is higher (D35). */
  async allVersions(name: string): Promise<VocabularyVersion[]> {
    const first = await this.listVersions(name, 1);
    const items = [...first.items];
    for (let page = 2; page <= first.totalPages; page++) {
      items.push(...(await this.listVersions(name, page)).items);
    }
    return items;
  }

  async changeStatus(name: string, status: string): Promise<Vocabulary> {
    return (
      await this.request(`/vocabularies/${encodeURIComponent(name)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
    ).json();
  }

  async createVocabulary(name: string, description: string, file: Blob, fileName: string): Promise<CreatedVersion> {
    const form = new FormData();
    form.append('name', name);
    form.append('description', description);
    form.append('changelog', 'Initial version.');
    form.append('vocabularyFile', file, fileName);
    return (await this.request('/vocabularies', { method: 'POST', body: form })).json();
  }

  async createVersion(
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
      await this.request(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: 'POST', body: form })
    ).json();
  }

  async content(name: string, version?: number): Promise<Blob> {
    const path =
      version === undefined
        ? `/content/${encodeURIComponent(name)}`
        : `/content/${encodeURIComponent(name)}/versions/${version}`;
    return (await this.request(path, {}, 'text/turtle')).blob();
  }
}

/** Saves a blob as a file download. */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
