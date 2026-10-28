import { config } from './config';
import { token } from './auth';

export type SchemaType = 'RESOURCE' | 'SEARCH';

export interface SchemaItem {
  name: string;
  title: string;
  description: string;
  resourceType: string | null;
  schemaType: SchemaType;
  version: string;
  changelog: string;
  status: 'PUBLISHED' | 'REVOKED';
  created: string;
  modified: string;
  createdBy: string | null;
}

export interface VersionItem {
  name: string;
  version: string;
  changelog: string;
  created: string;
  createdBy: string | null;
  schemaType: SchemaType;
  resourceType: string | null;
  status: 'PUBLISHED' | 'REVOKED';
  graphName: string;
  title?: string;
  description?: string;
}

export interface PageResult<T> {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
  next: string | null;
  metadata: T[];
}

/** An API error carrying the HTTP status and the problem detail. */
export class ApiError extends Error {
  constructor(public status: number, public detail: string) {
    super(detail);
  }
}

async function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${await token()}`);
  let response: Response;
  try {
    response = await fetch(config.apiBase + path, { ...init, headers });
  } catch (e) {
    throw new ApiError(0, 'The server could not be reached.');
  }
  if (!response.ok) {
    let detail = `Request failed with status ${response.status}.`;
    try {
      const body = await response.json();
      if (body && typeof body.detail === 'string') {
        detail = body.detail;
      }
    } catch {
      // Not a problem+json body; keep the generic text.
    }
    throw new ApiError(response.status, detail);
  }
  return response;
}

function query(params: Record<string, string | number | undefined | null>): string {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') {
      q.set(k, String(v));
    }
  }
  const s = q.toString();
  return s ? `?${s}` : '';
}

const enc = encodeURIComponent;

export async function listSchemas(params: {
  schemaType: SchemaType;
  status?: string;
  resourceType?: string;
  page: number;
  pageSize: number;
}): Promise<PageResult<SchemaItem>> {
  const r = await request(`/schemas${query(params)}`, { headers: { Accept: 'application/json' } });
  return r.json();
}

export async function listAllVersions(name: string, schemaType: SchemaType): Promise<VersionItem[]> {
  const all: VersionItem[] = [];
  let page = 1;
  for (;;) {
    const r = await request(`/schemas/${enc(name)}/versions${query({ schemaType, page, pageSize: 100 })}`, {
      headers: { Accept: 'application/json' },
    });
    const body: PageResult<VersionItem> = await r.json();
    all.push(...body.metadata);
    if (!body.next || page >= body.totalPages) {
      return all;
    }
    page += 1;
  }
}

export async function createSchema(form: FormData): Promise<{ schemaName: string; schemaVersion: string }> {
  const r = await request('/schemas', { method: 'POST', body: form, headers: { Accept: 'application/json' } });
  return r.json();
}

export async function createVersion(name: string, form: FormData): Promise<{ version: string }> {
  const r = await request(`/schemas/${enc(name)}/versions`, {
    method: 'POST',
    body: form,
    headers: { Accept: 'application/json' },
  });
  return r.json();
}

export async function changeStatus(name: string, schemaType: SchemaType, status: 'PUBLISHED' | 'REVOKED'): Promise<void> {
  await request(`/schemas/${enc(name)}${query({ schemaType })}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ status }),
  });
}

/** Latest content (D9). The version is taken from Content-Location (D56). */
export async function downloadLatest(name: string, schemaType: SchemaType): Promise<{ content: Blob; version: string }> {
  const r = await request(`/schemas/${enc(name)}${query({ schemaType })}`, { headers: { Accept: 'text/turtle' } });
  const location = r.headers.get('Content-Location') ?? '';
  return { content: await r.blob(), version: versionFromUri(location) };
}

export async function downloadVersion(name: string, version: string, schemaType: SchemaType): Promise<Blob> {
  const r = await request(`/schemas/${enc(name)}/${enc(version)}${query({ schemaType })}`, {
    headers: { Accept: 'text/turtle' },
  });
  return r.blob();
}

export function versionFromUri(uri: string): string {
  try {
    const path = new URL(uri, window.location.origin).pathname;
    const parts = path.split('/').filter((p) => p.length > 0);
    return decodeURIComponent(parts[parts.length - 1] ?? '');
  } catch {
    return '';
  }
}
