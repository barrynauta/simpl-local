import { vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import PrimeVue from 'primevue/config';
import { createMemoryHistory } from 'vue-router';
import type { Component } from 'vue';
import { setSession, ADMIN_ROLE, VIEWER_ROLE } from '../src/auth';
import { makeRouter } from '../src/router';

export interface Call {
  url: string;
  init: RequestInit;
}

export function asUser(admin: boolean): { login: ReturnType<typeof vi.fn> } {
  const login = vi.fn();
  setSession({
    token: async () => 'test-token',
    roles: () => [admin ? ADMIN_ROLE : VIEWER_ROLE],
    login,
    logout: vi.fn(),
    username: () => 'tester',
  });
  return { login };
}

export function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}) {
  const type = status >= 400 ? 'application/problem+json' : 'application/json';
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': type, ...headers },
  });
}

/** Installs a fetch mock that answers through the handler and records every call. */
export function mockFetch(handler: (url: string, init: RequestInit) => Response): Call[] {
  const calls: Call[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init: RequestInit = {}) => {
      calls.push({ url, init });
      return handler(url, init);
    }),
  );
  return calls;
}

export async function mountAt(path: string, component: Component) {
  const router = makeRouter(createMemoryHistory());
  await router.push(path);
  await router.isReady();
  const wrapper = mount(component, {
    global: { plugins: [router, [PrimeVue, { unstyled: true }]] },
    attachTo: document.body,
  });
  await flushPromises();
  return { wrapper, router };
}

export function page<T>(items: T[], total = items.length, pageNumber = 1, pageSize = 20) {
  return {
    page: pageNumber,
    pageSize,
    total,
    totalPages: Math.ceil(total / pageSize),
    items,
  };
}

export const vocab = (name: string, status = 'ACTIVE', version = 1) => ({
  href: `/vocabularies/${name}`,
  contentHref: `https://authority.example/vocabulary-manager/content/${name}`,
  name,
  version,
  description: `Description of ${name}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-19T10:05:00Z',
});

export const version = (name: string, n: number, status = 'ACTIVE') => ({
  href: `/vocabularies/${name}/versions/${n}`,
  contentHref: `https://authority.example/vocabulary-manager/content/${name}/versions/${n}`,
  latestVersionHref: `/vocabularies/${name}/versions/3`,
  name,
  version: n,
  description: `Version ${n} description`,
  changelog: n === 1 ? 'Initial version.' : `Change ${n}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
});
