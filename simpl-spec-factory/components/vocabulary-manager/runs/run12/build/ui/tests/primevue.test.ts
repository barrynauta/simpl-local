import { describe, expect, it, vi } from 'vitest';
import { apiMock } from './mockApi';
import { mountAt, signIn, vocab } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  api: (await import('./mockApi')).apiMock,
}));

describe('PrimeVue unstyled (D123, D167)', () => {
  it('injects no style elements, so the CSP needs no unsafe-inline', async () => {
    signIn(true);
    apiMock.listVocabularies.mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [vocab('Health')],
    });
    const { wrapper } = await mountAt('/vocabularies');
    await wrapper.find('button[aria-label="Actions for Health"]').trigger('click');
    expect(document.querySelectorAll('style').length).toBe(0);
    const items = Array.from(document.querySelectorAll('[role="menuitem"]')).map((e) =>
      e.textContent?.trim(),
    );
    expect(items).toEqual([
      'View vocabulary details',
      'History',
      'Download vocabulary',
      'Update',
      'Deprecate',
    ]);
  });
});
