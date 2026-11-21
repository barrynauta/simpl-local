import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { apiMock, resetApiMock } from './mockApi';
import { mountAt, signIn, vocab } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  api: (await import('./mockApi')).apiMock,
}));

const { ApiError } = await import('../src/api');

const page = (items: unknown[]) => ({
  page: 1,
  pageSize: 20,
  total: items.length,
  totalPages: items.length ? 1 : 0,
  items,
});

describe('Vocabulary list view', () => {
  beforeEach(() => {
    resetApiMock();
    document.body.innerHTML = '';
  });

  it('shows the decided columns, rows and deprecation icon (D100, D159, D72)', async () => {
    signIn(true);
    apiMock.listVocabularies.mockResolvedValue(
      page([vocab('Health', 'ACTIVE', 3), vocab('Legacy', 'DEPRECATED')]),
    );
    const { wrapper } = await mountAt('/vocabularies');
    const headers = wrapper.findAll('th').map((th) => th.text().replace(/[▲▼]/g, ''));
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
    expect(wrapper.text()).toContain('2026-11-18 09:27 UTC');
    const icon = wrapper.find('[role="img"]');
    expect(icon.attributes('aria-label')).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.find('button[aria-label="Actions for Health"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('New Vocabulary +');
    expect(wrapper.text()).toContain('Page 1 of 1');
    expect(apiMock.listVocabularies).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1, pageSize: 20, sort: 'name' }),
    );
  });

  it('reverses the active sort column and sorts others ascending (D50, D163)', async () => {
    signIn(false);
    apiMock.listVocabularies.mockResolvedValue(page([vocab('Health')]));
    const { wrapper } = await mountAt('/vocabularies');
    const nameTh = wrapper.findAll('th')[0];
    expect(nameTh.attributes('aria-sort')).toBe('ascending');
    await nameTh.find('button').trigger('click');
    await flushPromises();
    expect(apiMock.listVocabularies).toHaveBeenLastCalledWith(
      expect.objectContaining({ sort: '-name' }),
    );
    expect(wrapper.findAll('th')[0].attributes('aria-sort')).toBe('descending');
    await wrapper.findAll('th')[1].find('button').trigger('click');
    await flushPromises();
    expect(apiMock.listVocabularies).toHaveBeenLastCalledWith(
      expect.objectContaining({ sort: 'status' }),
    );
  });

  it('hides admin actions for a viewer and shows the empty state (D18, D34)', async () => {
    signIn(false);
    apiMock.listVocabularies.mockResolvedValue(page([]));
    const { wrapper } = await mountAt('/vocabularies');
    expect(wrapper.text()).not.toContain('New Vocabulary +');
    expect(wrapper.text()).toContain('No vocabularies found.');
  });

  it('shows one alert with the load error and the detail (D34, D114)', async () => {
    signIn(false);
    apiMock.listVocabularies.mockRejectedValue(new ApiError(500, { detail: 'Store failed' }));
    const { wrapper } = await mountAt('/vocabularies');
    const alerts = wrapper.findAll('[role="alert"]');
    expect(alerts).toHaveLength(1);
    expect(alerts[0].text()).toContain('The vocabulary list could not be loaded.');
    expect(alerts[0].text()).toContain('Store failed');
  });

  it('filters by status (SIMPL-18163)', async () => {
    signIn(false);
    apiMock.listVocabularies.mockResolvedValue(page([vocab('Health')]));
    const { wrapper } = await mountAt('/vocabularies');
    await wrapper.find('#filter-status').setValue('DEPRECATED');
    await flushPromises();
    expect(apiMock.listVocabularies).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'DEPRECATED', page: 1 }),
    );
  });
});
