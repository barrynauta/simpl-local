import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import VocabularyList from '../src/views/VocabularyList.vue';
import { api, ApiError, type Vocabulary } from '../src/api';
import { flush, testRouter, useSession } from './helpers';

const item = (name: string, status: 'ACTIVE' | 'DEPRECATED'): Vocabulary => ({
  href: `/vocabularies/${name}`,
  contentHref: '',
  name,
  version: 2,
  description: `About ${name}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
});

describe('Vocabulary list', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('shows rows, the deprecation icon name and sorts as D50 says', async () => {
    useSession(true);
    const list = vi.spyOn(api, 'listVocabularies').mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 2,
      totalPages: 1,
      items: [item('Alpha', 'ACTIVE'), item('Beta', 'DEPRECATED')],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    expect(wrapper.text()).toContain('Alpha');
    expect(wrapper.text()).toContain('2026-11-18 09:27 UTC');
    expect(wrapper.find('[role="img"]').attributes('aria-label')).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.text()).toContain('New Vocabulary +');
    const nameHeader = wrapper.findAll('th button').find((b) => b.text() === 'Name');
    await nameHeader?.trigger('click');
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: '-name' }));
    const statusHeader = wrapper.findAll('th button').find((b) => b.text() === 'Status');
    await statusHeader?.trigger('click');
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'status' }));
  });

  it('shows the empty state and hides admin actions for a viewer', async () => {
    useSession(false);
    vi.spyOn(api, 'listVocabularies').mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 0,
      totalPages: 0,
      items: [],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    expect(wrapper.text()).toContain('No vocabularies found.');
    expect(wrapper.text()).not.toContain('New Vocabulary +');
  });

  it('shows the load error with the problem detail', async () => {
    useSession(true);
    vi.spyOn(api, 'listVocabularies').mockRejectedValue(new ApiError({ status: 503, detail: 'Store down' }));
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toContain('The vocabulary list could not be loaded.');
    expect(wrapper.text()).toContain('Store down');
  });

  it('changes the status from the actions menu', async () => {
    useSession(true);
    vi.spyOn(api, 'listVocabularies').mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [item('Alpha', 'ACTIVE')],
    });
    const patch = vi.spyOn(api, 'changeStatus').mockResolvedValue(item('Alpha', 'DEPRECATED'));
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] }, attachTo: document.body });
    await flush();
    await wrapper.find('button[aria-label="Actions for Alpha"]').trigger('click');
    const deprecate = wrapper.findAll('[role="menuitem"]').find((b) => b.text() === 'Deprecate');
    await deprecate?.trigger('click');
    await flush();
    expect(patch).toHaveBeenCalledWith('Alpha', 'DEPRECATED');
    expect(wrapper.find('[role="status"]').text()).toBe('Status of Alpha changed to Deprecated.');
    patch.mockRejectedValue(new ApiError({ status: 404 }));
    await wrapper.find('button[aria-label="Actions for Alpha"]').trigger('click');
    await wrapper
      .findAll('[role="menuitem"]')
      .find((b) => b.text() === 'Deprecate')
      ?.trigger('click');
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toContain(
      'Vocabulary not found. The status was not changed.',
    );
    wrapper.unmount();
  });
});
