import { bearer } from './auth';

export class ApiError extends Error {
  constructor(public status: number, public detail: string) {
    super(detail);
  }
}

async function request(path: string, init: RequestInit = {}, accept = 'application/json'): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', await bearer());
  headers.set('Accept', `${accept}, application/problem+json`);
  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const p = await res.json();
      detail = p.detail || p.title || detail;
    } catch {
      /* not JSON */
    }
    throw new ApiError(res.status, detail);
  }
  return res;
}

export async function getJson<T = any>(path: string): Promise<T> {
  return (await request(path)).json();
}

export async function postJson<T = any>(path: string, body: unknown): Promise<T> {
  const res = await request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return res.json();
}

export async function getText(path: string): Promise<string> {
  return (await request(path, {}, 'text/turtle')).text();
}

export interface Catalogue { id: string; name: string; authorityURL: string; active: boolean }

export interface ResultItem {
  i: {
    claimsGraphUri: string[]; offeringType: string | null; name: string | null; description: string | null;
    inLanguage: string | null; serviceAccessPoint: string | null; qualityScore: number | null;
    qualityClassification: string | null; catalogueId: string;
  };
}

export interface Group {
  catalogueId: string; catalogueName: string; governanceAuthorityId: string; authorityURL: string | null;
  local: boolean; status: 'OK' | 'UNAUTHENTICATED' | 'UNAVAILABLE'; error: { type: string; detail: string } | null;
  page: number; pageSize: number; total: number; totalPages: number; items: ResultItem[];
}

export interface SearchResponse { totalCount: number; items: ResultItem[]; groups: Group[] }

export interface SchemaInfo { schemaId: string; name: string; title: string; schemaType: string; resourceType: string | null; version: string }

/** Labels of the active profile, ordered by the lower bound of their score range (D53, D72). */
export async function classificationLabels(): Promise<string[]> {
  const profile = await getJson('/api/v2/schemas/DataOfferingShape/qualityProfiles/active');
  const ranges: { label: string; min: number }[] = [];
  const visit = (n: any) => {
    if (Array.isArray(n)) return n.forEach(visit);
    if (!n || typeof n !== 'object') return;
    const types = ([] as string[]).concat(n['@type'] || []);
    if (types.some((t) => t.endsWith('ScoreRange'))) {
      const c = n.rangeClassification;
      const iri: string = typeof c === 'string' ? c : c?.['@id'];
      const min = typeof n.minScore === 'object' ? Number(n.minScore['@value']) : Number(n.minScore);
      if (iri) ranges.push({ label: iri.split(/[/#:]/).pop() as string, min });
    }
    Object.entries(n).forEach(([k, v]) => k !== '@context' && visit(v));
  };
  visit(profile);
  ranges.sort((a, b) => a.min - b.min);
  return [...new Set(ranges.map((r) => r.label))];
}
