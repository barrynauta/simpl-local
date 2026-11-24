import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import PrimeVue from 'primevue/config';
import { createMemoryHistory } from 'vue-router';
import { vi } from 'vitest';
import { defineComponent, h } from 'vue';
import { RouterView } from 'vue-router';
import { session } from '../src/lib/auth';
import { makeRouter } from '../src/router';

/** Signs a test user in with the given roles. */
export function signIn(roles: string[]): void {
  session.authenticated = true;
  session.username = 'tester';
  session.roles = roles;
}

/** A JSON or problem response. */
export function reply(status: number, body: unknown, type = 'application/json'): Response {
  return new Response(typeof body === 'string' ? body : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : type },
  });
}

/** Stubs fetch with a handler of method and path. */
export function stubFetch(handler: (method: string, path: string, init?: RequestInit) => Response) {
  const mock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString();
    return handler((init?.method ?? 'GET').toUpperCase(), url, init);
  });
  vi.stubGlobal('fetch', mock);
  return mock;
}

/** Mounts the app shell at a route. */
export async function mountAt(path: string): Promise<VueWrapper> {
  const router = makeRouter(createMemoryHistory());
  await router.push(path);
  await router.isReady();
  const Shell = defineComponent({ render: () => h(RouterView) });
  const wrapper = mount(Shell, {
    global: { plugins: [router, [PrimeVue, { unstyled: true }]] },
    attachTo: document.body,
  });
  await flushPromises();
  return wrapper;
}

export const ACTIVE = {
  href: '/vocabularies/Health',
  contentHref: 'https://authority.example/vocabulary-manager/content/Health',
  name: 'Health',
  version: 2,
  description: 'Health terms',
  status: 'ACTIVE',
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-19T10:00:00Z',
};

export function version(n: number, status: string) {
  return {
    href: `/vocabularies/Health/versions/${n}`,
    contentHref: `https://authority.example/vocabulary-manager/content/Health/versions/${n}`,
    name: 'Health',
    version: n,
    description: `Description ${n}`,
    changelog: `Change ${n}`,
    status,
    createdBy: 'ga-admin@example.com',
    creationDate: '2026-11-18T09:27:00Z',
    lastUpdateDate: '2026-11-18T09:27:00Z',
  };
}

export function page<T>(items: T[]) {
  return { page: 1, pageSize: 20, total: items.length, totalPages: items.length ? 1 : 0, items };
}
