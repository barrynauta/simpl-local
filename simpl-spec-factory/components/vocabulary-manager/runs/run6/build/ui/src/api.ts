import { accessToken } from "./auth";

/** Problem details body of the API (RFC 9457). */
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

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: "ACTIVE" | "DEPRECATED";
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
  status: "ACTIVE" | "DEPRECATED";
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
export interface PageOf<T> {
  page: number;
  totalPages: number;
  total: number;
  pageSize: number;
  items: T[];
}

const BASE = "/v1";

async function request(path: string, init: RequestInit = {}, accept = "application/json"): Promise<Response> {
  const token = await accessToken();
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.set("Accept", accept);
  const response = await fetch(BASE + path, { ...init, headers });
  if (!response.ok) {
    let problem: Problem = { status: response.status };
    try {
      problem = (await response.json()) as Problem;
    } catch {
      // Body is not JSON; keep the status only.
    }
    throw new ApiError(response.status, problem);
  }
  return response;
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  return (await (await request(path, init)).json()) as T;
}

export function listVocabularies(query: URLSearchParams): Promise<PageOf<Vocabulary>> {
  return json(`/vocabularies?${query.toString()}`);
}

export function getVocabulary(name: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`);
}

/** Loads every version, 100 per page, newest first (decision D35). */
export async function allVersions(name: string): Promise<VocabularyVersion[]> {
  const versions: VocabularyVersion[] = [];
  for (let page = 1; ; page++) {
    const result = await json<PageOf<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
    );
    versions.push(...result.items);
    if (page >= result.totalPages) {
      return versions;
    }
  }
}

export function createVocabulary(form: FormData): Promise<CreatedVersion> {
  return json("/vocabularies", { method: "POST", body: form });
}

export function createVersion(name: string, form: FormData): Promise<CreatedVersion> {
  return json(`/vocabularies/${encodeURIComponent(name)}/versions`, { method: "POST", body: form });
}

export function changeStatus(name: string, status: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
}

/** Fetches content as a Blob (latest version when version is undefined). */
export async function fetchContent(name: string, version?: number): Promise<Blob> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`;
  return (await request(path, {}, "text/turtle")).blob();
}

/** Saves content as a file download with the decided file name (decision D29). */
export async function downloadContent(name: string, version?: number): Promise<void> {
  const blob = await fetchContent(name, version);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
