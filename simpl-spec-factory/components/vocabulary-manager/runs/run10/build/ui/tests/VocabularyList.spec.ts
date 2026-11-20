import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import VocabularyList from '../src/views/VocabularyList.vue';
import * as api from '../src/api';
import { flush, routerAt, signInAs } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  listVocabularies: vi.fn(),
  changeStatus: vi.fn(),
  download: vi.fn(),
}));

const row = (name: string, status: api.Status): api.Vocabulary => ({
  href: `/vocabularies/${name}`,
  contentHref: '',
  name,
  version: 2,
  description: `About ${name}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-19T10:00:00Z',
});

describe('VocabularyList', () => {
  beforeEach(() => {
    vi.mocked(api.listVocabularies).mockClear();
  });

  it('shows the decided columns, the deprecation icon and admin actions', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      totalPages: 1,
      total: 2,
      pageSize: 20,
      items: [row('Health', 'ACTIVE'), row('Legacy', 'DEPRECATED')],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [await routerAt('/vocabularies')] } });
    await flush();
    const headers = wrapper.findAll('th').map((th) => th.text().replace(/[▲▼]/g, '').trim());
    expect(headers).toEqual([
      'Name',
      'Status',
      'Description',
      'Creator',
      'Date created',
      'Last updated',
      'Actions',
    ]);
    expect(wrapper.text()).toContain('2026-11-18 09:27 UTC');
    expect(wrapper.find('[role="img"]').attributes('aria-label')).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.text()).toContain('New Vocabulary +');
    expect(wrapper.text()).toContain('Page 1 of 1');
    await wrapper.find('button[aria-label="Actions for Health"]').trigger('click');
    const items = wrapper.findAll('[role="menuitem"]').map((item) => item.text());
    expect(items).toEqual([
      'View vocabulary details',
      'History',
      'Download vocabulary',
      'Update',
      'Deprecate',
    ]);
  });

  it('sorts descending on a second click and hides admin actions for a viewer', async () => {
    signInAs(['GA_VOCABULARY_VIEWER']);
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      totalPages: 0,
      total: 0,
      pageSize: 20,
      items: [],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [await routerAt('/vocabularies')] } });
    await flush();
    expect(wrapper.text()).toContain('No vocabularies found.');
    expect(wrapper.text()).not.toContain('New Vocabulary +');
    await wrapper.findAll('button.sort')[0].trigger('click');
    expect(vi.mocked(api.listVocabularies).mock.calls.at(-1)?.[0].sort).toBe('-name');
  });

  it('shows one alert with the load error and the problem detail', async () => {
    signInAs(['GA_VOCABULARY_VIEWER']);
    vi.mocked(api.listVocabularies).mockImplementation(async () => {
      throw new api.ApiError(500, { detail: 'Boom' });
    });
    const router = await routerAt('/vocabularies');
    const wrapper = mount(VocabularyList, { global: { plugins: [router] } });
    await flush();
    const alerts = wrapper.findAll('[role="alert"]');
    expect(alerts).toHaveLength(1);
    expect(alerts[0].text()).toBe('The vocabulary list could not be loaded. Boom');
  });

  it('reports a status change', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      totalPages: 1,
      total: 1,
      pageSize: 20,
      items: [row('Health', 'ACTIVE')],
    });
    vi.mocked(api.changeStatus).mockResolvedValue(row('Health', 'DEPRECATED'));
    const wrapper = mount(VocabularyList, { global: { plugins: [await routerAt('/vocabularies')] } });
    await flush();
    await wrapper.find('button[aria-label="Actions for Health"]').trigger('click');
    await wrapper.findAll('[role="menuitem"]').at(-1)?.trigger('click');
    await flush();
    expect(wrapper.find('[role="status"]').text()).toBe('Status of Health changed to Deprecated.');
    vi.mocked(api.changeStatus).mockImplementation(async () => {
      throw new api.ApiError(404, {});
    });
    await wrapper.find('button[aria-label="Actions for Health"]').trigger('click');
    await wrapper.findAll('[role="menuitem"]').at(-1)?.trigger('click');
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toBe('Vocabulary not found. The status was not changed.');
  });
});
