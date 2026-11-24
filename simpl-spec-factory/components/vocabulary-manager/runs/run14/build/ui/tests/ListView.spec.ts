import { flushPromises } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ACTIVE, mountAt, page, reply, signIn, stubFetch } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('Vocabulary list', () => {
  it('shows the decided columns and the deprecation icon', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    stubFetch(() => reply(200, page([ACTIVE, { ...ACTIVE, name: 'Legacy', status: 'DEPRECATED' }])));
    const wrapper = await mountAt('/vocabularies');
    const headers = wrapper.findAll('th').map((th) => th.text().replace(/[▲▼↕]/g, '').trim());
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
    expect(wrapper.find('[role="img"]').attributes('aria-label')).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.text()).toContain('2026-11-18 09:27 UTC');
    expect(wrapper.text()).not.toContain('New Vocabulary +');
  });

  it('shows the empty state and the load error', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    stubFetch(() => reply(200, page([])));
    let wrapper = await mountAt('/vocabularies');
    expect(wrapper.text()).toContain('No vocabularies found.');
    expect(wrapper.text()).toContain('New Vocabulary +');
    wrapper.unmount();
    stubFetch(() => reply(500, { status: 500, detail: 'Internal Server Error' }));
    wrapper = await mountAt('/vocabularies');
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'The vocabulary list could not be loaded. Internal Server Error',
    );
  });

  it('reverses the active sort column on click and sends the sort', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    const fetchMock = stubFetch(() => reply(200, page([ACTIVE])));
    const wrapper = await mountAt('/vocabularies');
    const nameHeader = wrapper.findAll('th')[0];
    expect(nameHeader.attributes('aria-sort')).toBe('ascending');
    await nameHeader.find('button').trigger('click');
    await flushPromises();
    expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('sort=-name');
    expect(wrapper.findAll('th')[0].attributes('aria-sort')).toBe('descending');
  });

  it('asks for confirmation before a status change and reports success', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    const fetchMock = stubFetch((method) =>
      method === 'PATCH' ? reply(200, { ...ACTIVE, status: 'DEPRECATED' }) : reply(200, page([ACTIVE])),
    );
    const wrapper = await mountAt('/vocabularies');
    await wrapper.find('button[aria-label="Actions for Health"]').trigger('click');
    const items = wrapper.findAll('[role="menuitem"]').map((item) => item.text());
    expect(items).toEqual([
      'View vocabulary details',
      'History',
      'Download vocabulary',
      'Update',
      'Deprecate',
    ]);
    await wrapper.findAll('[role="menuitem"]')[4].trigger('click');
    await flushPromises();
    expect(document.body.textContent).toContain("Do you want to deprecate 'Health'?");
    const confirm = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent === 'Confirm',
    );
    confirm?.click();
    await flushPromises();
    expect(
      fetchMock.mock.calls.some((call) => (call[1] as RequestInit | undefined)?.method === 'PATCH'),
    ).toBe(true);
    expect(wrapper.find('[role="status"]').text()).toBe('Status of Health changed to Deprecated.');
  });
});
