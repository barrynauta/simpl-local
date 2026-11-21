import { defineComponent, h } from 'vue';
import { RouterView, createMemoryHistory } from 'vue-router';
import { flushPromises, mount } from '@vue/test-utils';
import { installPrimeVue } from '../src/primevue';
import { makeRouter } from '../src/router';
import { configureSession, ROLE_ADMIN, ROLE_VIEWER } from '../src/auth';

export function signIn(admin: boolean) {
  configureSession({
    username: admin ? 'ga-admin' : 'ga-viewer',
    roles: [admin ? ROLE_ADMIN : ROLE_VIEWER],
    tokenProvider: async () => 'token',
    logout: () => undefined,
  });
}

export async function mountAt(path: string) {
  const router = makeRouter(createMemoryHistory());
  await router.push(path);
  await router.isReady();
  const Root = defineComponent({ render: () => h(RouterView) });
  const wrapper = mount(Root, {
    attachTo: document.body,
    global: { plugins: [router, { install: installPrimeVue }] },
  });
  await flushPromises();
  return { wrapper, router };
}

export const vocab = (name: string, status: 'ACTIVE' | 'DEPRECATED' = 'ACTIVE', version = 1) => ({
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

export const version = (name: string, n: number, status: 'ACTIVE' | 'DEPRECATED') => ({
  href: `/vocabularies/${name}/versions/${n}`,
  contentHref: `https://authority.example/vocabulary-manager/content/${name}/versions/${n}`,
  latestVersionHref: `/vocabularies/${name}/versions/3`,
  name,
  version: n,
  description: `Description v${n}`,
  changelog: `Change ${n}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
});
