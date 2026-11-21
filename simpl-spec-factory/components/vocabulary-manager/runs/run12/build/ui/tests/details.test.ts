import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiMock, resetApiMock } from './mockApi';
import { mountAt, signIn, version, vocab } from './helpers';
import { setFlash } from '../src/flash';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  api: (await import('./mockApi')).apiMock,
}));

const { ApiError } = await import('../src/api');

describe('Vocabulary details view', () => {
  beforeEach(() => {
    resetApiMock();
    document.body.innerHTML = '';
  });

  it('lists versions with deprecation warnings and the latest ACTIVE link (D35, D36, D118)', async () => {
    signIn(true);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health', 'ACTIVE', 3));
    apiMock.listAllVersions.mockResolvedValue([
      version('Health', 3, 'DEPRECATED'),
      version('Health', 2, 'ACTIVE'),
      version('Health', 1, 'DEPRECATED'),
    ]);
    const { wrapper } = await mountAt('/vocabularies/Health');
    expect(wrapper.find('h1').text()).toBe('Health');
    expect(wrapper.findAll('th').map((th) => th.text())).toEqual([
      'Version',
      'Status',
      'Date created',
      'Description',
      'Change log',
      'Actions',
    ]);
    expect(wrapper.text()).toContain('This version is deprecated.');
    const links = wrapper.findAll('a').filter((a) => a.text() === 'Latest ACTIVE version: 2');
    expect(links).toHaveLength(2);
    expect(wrapper.find('button[aria-label="Actions for version 3"]').exists()).toBe(true);
  });

  it('opens the version panel with its metadata (D35, D163)', async () => {
    signIn(false);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health', 'DEPRECATED', 2));
    apiMock.listAllVersions.mockResolvedValue([
      version('Health', 2, 'ACTIVE'),
      version('Health', 1, 'DEPRECATED'),
    ]);
    const { wrapper } = await mountAt('/vocabularies/Health/versions/1');
    const panel = wrapper.find('section[aria-label="Version 1"]');
    expect(panel.exists()).toBe(true);
    expect(panel.text()).toContain('ga-admin@example.com');
    expect(panel.text()).toContain('Change 1');
    expect(panel.text()).not.toContain('Latest ACTIVE version');
    expect(wrapper.find('tr[aria-current="true"]').exists()).toBe(true);
    const banner = wrapper.findAll('[role="status"]').map((s) => s.text());
    expect(banner).toContain('This vocabulary is deprecated. Its use is discouraged.');
    expect(wrapper.text()).not.toContain('Update');
  });

  it('shows the creation notification and warnings (D14, D72, D109)', async () => {
    signIn(true);
    setFlash('Health', 'Version 1 of Health was created.', [
      { code: 'externalReferenceSkipped', description: 'Skipped.', location: "prefix 'dc'" },
    ]);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health'));
    apiMock.listAllVersions.mockResolvedValue([version('Health', 1, 'ACTIVE')]);
    const { wrapper } = await mountAt('/vocabularies/Health');
    expect(wrapper.text()).toContain('Version 1 of Health was created.');
    const region = wrapper.find('section[aria-label="Validation warnings"]');
    expect(region.find('[role="status"]').text()).toBe('1 warning found');
  });

  it('shows not found on 404 (D35)', async () => {
    signIn(false);
    apiMock.getVocabulary.mockRejectedValue(new ApiError(404, { detail: 'x' }));
    apiMock.listAllVersions.mockRejectedValue(new ApiError(404, { detail: 'x' }));
    const { wrapper } = await mountAt('/vocabularies/Missing');
    expect(wrapper.find('[role="alert"]').text()).toBe('Vocabulary not found.');
    expect(wrapper.text()).toContain('Back to vocabulary list');
  });
});
