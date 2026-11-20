import { session } from "./auth";

/** The API base: same origin, proxied by nginx (MACH-04: the UI uses the public API). */
export const API = "/v1";

export interface ProblemIssue {
  detail?: string;
  location?: string;
  in?: string;
  name?: string;
}

export interface Problem {
  status?: number;
  title?: string;
  detail?: string;
  issues?: ProblemIssue[];
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Problem,
  ) {
    super(problem.detail ?? `HTTP ${status}`);
  }
}

export type Status = "ACTIVE" | "DEPRECATED";

export interface Vocabulary {
  href: string;
  contentHref: string;
  name: string;
  version: number;
  description: string;
  status: Status;
  createdBy?: string;
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
  createdBy?: string;
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

export interface PageOf<T> {
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
  items: T[];
}

async function headers(extra: Record<string, string> = {}): Promise<Record<string, string>> {
  const s = session();
  const token = s ? await s.token() : "";
  return { Accept: "application/json", Authorization: `Bearer ${token}`, ...extra };
}

async function failure(response: Response): Promise<ApiError> {
  let problem: Problem;
  try {
    problem = (await response.json()) as Problem;
  } catch {
    problem = { status: response.status, detail: response.statusText };
  }
  return new ApiError(response.status, problem);
}

async function json<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(API + path, init);
  if (!response.ok) {
    throw await failure(response);
  }
  return (await response.json()) as T;
}

export async function listVocabularies(query: URLSearchParams): Promise<PageOf<Vocabulary>> {
  return json(`/vocabularies?${query.toString()}`, { headers: await headers() });
}

export async function getVocabulary(name: string): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`, { headers: await headers() });
}

/** All versions, newest first, 100 per page, further pages fetched when needed (D35). */
export async function getAllVersions(name: string): Promise<VocabularyVersion[]> {
  const all: VocabularyVersion[] = [];
  let page = 1;
  for (;;) {
    const result = await json<PageOf<VocabularyVersion>>(
      `/vocabularies/${encodeURIComponent(name)}/versions?page=${page}&pageSize=100`,
      { headers: await headers() },
    );
    all.push(...result.items);
    if (page >= result.totalPages) {
      return all;
    }
    page++;
  }
}

export async function setStatus(name: string, status: Status): Promise<Vocabulary> {
  return json(`/vocabularies/${encodeURIComponent(name)}`, {
    method: "PATCH",
    headers: await headers({ "Content-Type": "application/json" }),
    body: JSON.stringify({ status }),
  });
}

export async function createVocabulary(form: FormData): Promise<CreatedVersion> {
  return json(`/vocabularies`, { method: "POST", headers: await headers(), body: form });
}

export async function createVersion(name: string, form: FormData): Promise<CreatedVersion> {
  return json(`/vocabularies/${encodeURIComponent(name)}/versions`, {
    method: "POST",
    headers: await headers(),
    body: form,
  });
}

/** Content of the latest version or of one version (D29). */
export async function getContent(name: string, version?: number): Promise<Blob> {
  const path =
    version === undefined
      ? `/content/${encodeURIComponent(name)}`
      : `/content/${encodeURIComponent(name)}/versions/${version}`;
  const response = await fetch(API + path, { headers: await headers({ Accept: "text/turtle" }) });
  if (!response.ok) {
    throw await failure(response);
  }
  return response.blob();
}

export async function download(name: string, version?: number): Promise<void> {
  const blob = await getContent(name, version);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = version === undefined ? `${name}.ttl` : `${name}-v${version}.ttl`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
