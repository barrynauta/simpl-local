import { afterEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import VocabularyList from '../src/views/VocabularyList.vue';
import { asUser, jsonResponse, mockFetch, mountAt, page, vocab } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('Vocabulary list', () => {
  it('shows the decided columns, rows and the deprecation icon', async () => {
    asUser(false);
    const calls = mockFetch(() =>
      jsonResponse(page([vocab('Alpha'), vocab('Beta', 'DEPRECATED', 3)])),
    );
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    const headers = wrapper.findAll('th').map((th) => th.text().replace(/[↑↓↕]/g, '').trim());
    expect(headers).toEqual([
      'Name',
      'Status',
      'Latest version',
      'Description',
      'Creator',
      'Date created',
      'Last updated',
      'Actions',
    ]);
    expect(calls[0].url).toBe('/v1/vocabularies?sort=name&page=1&pageSize=20');
    expect(wrapper.text()).toContain('2026-11-18 09:27 UTC');
    const icon = wrapper.find('[role="img"]');
    expect(icon.attributes('aria-label')).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.find('[aria-label="Actions for Beta"]').exists()).toBe(true);
    expect(wrapper.text()).not.toContain('New Vocabulary +');
    expect(wrapper.text()).toContain('Page 1 of 1');
    expect(wrapper.find('th[aria-sort="ascending"]').text()).toContain('Name');
  });

  it('reverses the active sort column and sorts another column ascending (D50)', async () => {
    asUser(true);
    const calls = mockFetch(() => jsonResponse(page([vocab('Alpha')])));
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    expect(wrapper.text()).toContain('New Vocabulary +');
    const buttons = wrapper.findAll('th button');
    await buttons[0].trigger('click');
    await flushPromises();
    expect(calls.at(-1)?.url).toContain('sort=-name');
    await buttons[1].trigger('click');
    await flushPromises();
    expect(calls.at(-1)?.url).toContain('sort=status');
  });

  it('shows the empty text', async () => {
    asUser(true);
    mockFetch(() => jsonResponse(page([])));
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    expect(wrapper.text()).toContain('No vocabularies found.');
  });

  it('shows one alert with the load error text and the detail', async () => {
    asUser(true);
    mockFetch(() => jsonResponse({ status: 500, detail: 'Internal Server Error' }, 500));
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    const alerts = wrapper.findAll('[role="alert"]');
    expect(alerts).toHaveLength(1);
    expect(alerts[0].text()).toBe('The vocabulary list could not be loaded. Internal Server Error');
  });

  it('changes the status and confirms it', async () => {
    asUser(true);
    const calls = mockFetch((url, init) =>
      init.method === 'PATCH'
        ? jsonResponse(vocab('Alpha', 'DEPRECATED'))
        : jsonResponse(page([vocab('Alpha')])),
    );
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    const vm = wrapper.vm as unknown as { changeStatus: (v: unknown) => Promise<void> };
    await vm.changeStatus(vocab('Alpha'));
    await flushPromises();
    const patch = calls.find((c) => c.init.method === 'PATCH');
    expect(patch?.init.body).toBe('{"status":"DEPRECATED"}');
    expect(wrapper.find('[role="status"]').text()).toBe('Status of Alpha changed to Deprecated.');
  });

  it('reports a missing vocabulary on a failed status change', async () => {
    asUser(true);
    mockFetch((url, init) =>
      init.method === 'PATCH'
        ? jsonResponse({ status: 404, detail: 'gone' }, 404)
        : jsonResponse(page([vocab('Alpha', 'DEPRECATED')])),
    );
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    const vm = wrapper.vm as unknown as { changeStatus: (v: unknown) => Promise<void> };
    await vm.changeStatus(vocab('Alpha', 'DEPRECATED'));
    await flushPromises();
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'Vocabulary not found. The status was not changed.',
    );
  });

  it('offers admin actions in the row menu', async () => {
    asUser(true);
    mockFetch(() => jsonResponse(page([vocab('Alpha')])));
    const { wrapper } = await mountAt('/vocabularies', VocabularyList);
    const vm = wrapper.vm as unknown as { actionsFor: (v: unknown) => { label: string }[] };
    expect(vm.actionsFor(vocab('Alpha')).map((i) => i.label)).toEqual([
      'View vocabulary details',
      'History',
      'Download vocabulary',
      'Update',
      'Deprecate',
    ]);
    await wrapper.find('[aria-label="Actions for Alpha"]').trigger('click');
    await flushPromises();
    const items = Array.from(document.body.querySelectorAll('[role="menuitem"]')).map((e) =>
      e.getAttribute('aria-label'),
    );
    expect(items).toContain('Update');
  });
});
