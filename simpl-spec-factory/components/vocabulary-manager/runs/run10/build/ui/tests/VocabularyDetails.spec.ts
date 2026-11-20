import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import VocabularyDetails from '../src/views/VocabularyDetails.vue';
import * as api from '../src/api';
import { setNotice } from '../src/notice';
import { flush, routerAt, signInAs } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  getVocabulary: vi.fn(),
  listAllVersions: vi.fn(),
  download: vi.fn(),
  changeStatus: vi.fn(),
}));

const version = (n: number, status: api.Status): api.VocabularyVersion => ({
  href: `/vocabularies/Health/versions/${n}`,
  name: 'Health',
  version: n,
  description: `Description ${n}`,
  changelog: `Change ${n}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
  latestVersionHref: '/vocabularies/Health/versions/3',
});

function vocabulary(status: api.Status): api.Vocabulary {
  return {
    href: '/vocabularies/Health',
    contentHref: '',
    name: 'Health',
    version: 3,
    description: 'Description 3',
    status,
    createdBy: 'x',
    creationDate: '2026-11-18T09:27:00Z',
    lastUpdateDate: '2026-11-18T09:27:00Z',
  };
}

describe('VocabularyDetails', () => {
  it('warns on deprecated versions and links the latest ACTIVE version, never itself', async () => {
    signInAs(['GA_VOCABULARY_VIEWER']);
    vi.mocked(api.getVocabulary).mockResolvedValue(vocabulary('ACTIVE'));
    vi.mocked(api.listAllVersions).mockResolvedValue([
      version(3, 'DEPRECATED'),
      version(2, 'ACTIVE'),
      version(1, 'DEPRECATED'),
    ]);
    const wrapper = mount(VocabularyDetails, {
      props: { name: 'Health' },
      global: { plugins: [await routerAt('/vocabularies/Health')] },
    });
    await flush();
    const rows = wrapper.findAll('tbody tr');
    expect(rows[0].text().replace(/\s+/g, ' ')).toContain(
      'This version is deprecated. Latest ACTIVE version: 2',
    );
    expect(rows[1].text()).not.toContain('This version is deprecated.');
    await rows[2].find('a').trigger('click');
    await flush();
    expect(wrapper.find('section[aria-label="Version 2"]').exists()).toBe(true);
    expect(wrapper.text()).not.toContain('Update');
  });

  it('shows the banner and no recommendation for a deprecated vocabulary, and the creation notice', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    setNotice({
      name: 'Health',
      version: 3,
      warnings: [
        { code: 'externalReferenceSkipped', description: 'Cannot resolve', location: "prefix 'dc' -> 'x'" },
      ],
    });
    vi.mocked(api.getVocabulary).mockResolvedValue(vocabulary('DEPRECATED'));
    vi.mocked(api.listAllVersions).mockResolvedValue([version(3, 'ACTIVE')]);
    const wrapper = mount(VocabularyDetails, {
      props: { name: 'Health' },
      global: { plugins: [await routerAt('/vocabularies/Health')] },
    });
    await flush();
    expect(wrapper.text()).toContain('Version 3 of Health was created.');
    expect(wrapper.find('section[aria-label="Validation warnings"] [role="status"]').text()).toBe(
      '1 warning found',
    );
    expect(wrapper.find('.banner').text()).toBe('This vocabulary is deprecated. Its use is discouraged.');
    expect(wrapper.find('tbody tr').text()).toContain('This version is deprecated.');
    expect(wrapper.find('tbody tr').text()).not.toContain('Latest ACTIVE version');
    expect(wrapper.find('section[aria-label="Version 3"]').exists()).toBe(true);
  });

  it('shows not found with a link back', async () => {
    signInAs(['GA_VOCABULARY_VIEWER']);
    vi.mocked(api.getVocabulary).mockImplementation(async () => {
      throw new api.ApiError(404, {});
    });
    const wrapper = mount(VocabularyDetails, {
      props: { name: 'Missing' },
      global: { plugins: [await routerAt('/vocabularies/Missing')] },
    });
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toContain('Vocabulary not found.');
    expect(wrapper.find('[role="alert"] a').text()).toBe('Back to vocabulary list');
  });
});
