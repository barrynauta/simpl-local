import { afterEach, describe, expect, it, vi } from 'vitest';
import VocabularyDetails from '../src/views/VocabularyDetails.vue';
import { pendingNotice } from '../src/messages';
import { asUser, jsonResponse, mockFetch, mountAt, page, vocab, version } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

function serve(status: string, versions: ReturnType<typeof version>[]) {
  return mockFetch((url) =>
    url.includes('/versions')
      ? jsonResponse(page(versions, versions.length, 1, 100))
      : jsonResponse(vocab('Health', status, 3)),
  );
}

describe('Vocabulary details', () => {
  it('lists versions newest first with deprecation warnings and a link to the latest ACTIVE version', async () => {
    asUser(false);
    serve('ACTIVE', [
      version('Health', 3, 'ACTIVE'),
      version('Health', 2, 'DEPRECATED'),
      version('Health', 1, 'DEPRECATED'),
    ]);
    const { wrapper } = await mountAt('/vocabularies/Health', VocabularyDetails);
    expect(wrapper.find('h1').text()).toBe('Health');
    const headers = wrapper.findAll('th').map((th) => th.text());
    expect(headers).toEqual([
      'Version',
      'Status',
      'Date created',
      'Description',
      'Change log',
      'Actions',
    ]);
    const rows = wrapper.findAll('tbody tr');
    expect(rows[0].text()).not.toContain('This version is deprecated.');
    expect(rows[1].text()).toContain('This version is deprecated.');
    const link = rows[1].findAll('a').find((a) => a.text().startsWith('Latest ACTIVE'));
    expect(link?.text()).toBe('Latest ACTIVE version: 3');
    expect(link?.attributes('href')).toBe('/vocabularies/Health/versions/3');
    expect(wrapper.text()).toContain('Back to vocabulary list');
  });

  it('shows the banner and no recommendation for a deprecated vocabulary', async () => {
    asUser(false);
    serve('DEPRECATED', [version('Health', 3, 'ACTIVE'), version('Health', 2, 'DEPRECATED')]);
    const { wrapper } = await mountAt('/vocabularies/Health/versions/2', VocabularyDetails);
    expect(wrapper.find('[role="status"]').text()).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    expect(wrapper.text()).not.toContain('Latest ACTIVE version');
    const panel = wrapper.find('section[aria-label="Version 2"]');
    expect(panel.exists()).toBe(true);
    expect(panel.text()).toContain('This version is deprecated.');
    expect(panel.text()).toContain('ga-admin@example.com');
    expect(wrapper.find('tr[aria-current="true"]').text()).toContain('Change 2');
  });

  it('shows only the first sentence when no version is ACTIVE', async () => {
    asUser(false);
    serve('ACTIVE', [version('Health', 2, 'DEPRECATED'), version('Health', 1, 'DEPRECATED')]);
    const { wrapper } = await mountAt('/vocabularies/Health/versions/1', VocabularyDetails);
    expect(wrapper.find('section[aria-label="Version 1"]').text()).toContain(
      'This version is deprecated.',
    );
    expect(wrapper.text()).not.toContain('Latest ACTIVE version');
  });

  it('shows the creation notice and its warnings', async () => {
    asUser(true);
    pendingNotice.value = {
      message: 'Version 1 of Health was created.',
      warnings: [
        { code: 'externalReferenceSkipped', description: 'Skipped.', location: "prefix 'dc'" },
      ],
    };
    serve('ACTIVE', [version('Health', 1)]);
    const { wrapper } = await mountAt('/vocabularies/Health', VocabularyDetails);
    expect(wrapper.text()).toContain('Version 1 of Health was created.');
    const region = wrapper.find('section[aria-label="Validation warnings"]');
    expect(region.find('[role="status"]').text()).toBe('1 warning found');
    expect(region.text()).toContain('Skipped.');
  });

  it('shows the not-found text', async () => {
    asUser(false);
    mockFetch(() => jsonResponse({ status: 404, detail: 'nope' }, 404));
    const { wrapper } = await mountAt('/vocabularies/Missing', VocabularyDetails);
    expect(wrapper.find('[role="alert"]').text()).toBe('Vocabulary not found.');
  });

  it('shows the history load error with the detail', async () => {
    asUser(false);
    mockFetch(() => jsonResponse({ status: 503, detail: 'Store down.' }, 503));
    const { wrapper } = await mountAt('/vocabularies/Health', VocabularyDetails);
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'The version history could not be loaded. Store down.',
    );
  });
});
