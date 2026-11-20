import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import VocabularyDetails from '../src/views/VocabularyDetails.vue';
import { api, ApiError, type VocabularyVersion } from '../src/api';
import { setNotice } from '../src/notice';
import { flush, testRouter, useSession } from './helpers';

const version = (n: number, status: 'ACTIVE' | 'DEPRECATED'): VocabularyVersion => ({
  href: '',
  name: 'Alpha',
  version: n,
  description: `v${n}`,
  changelog: `c${n}`,
  status,
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
});

function vocabulary(status: 'ACTIVE' | 'DEPRECATED') {
  return {
    href: '',
    contentHref: '',
    name: 'Alpha',
    version: 3,
    description: 'v3',
    status,
    createdBy: 'x',
    creationDate: '2026-11-18T09:27:00Z',
    lastUpdateDate: '2026-11-18T09:27:00Z',
  };
}

describe('Vocabulary details', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('shows the history with deprecation warnings and the latest ACTIVE link', async () => {
    useSession(false);
    setNotice('Version 3 of Alpha was created.', [
      { code: 'externalReferenceSkipped', description: 'Skipped', location: 'prefix' },
    ]);
    vi.spyOn(api, 'getVocabulary').mockResolvedValue(vocabulary('ACTIVE'));
    vi.spyOn(api, 'listAllVersions').mockResolvedValue([
      version(3, 'DEPRECATED'),
      version(2, 'ACTIVE'),
      version(1, 'DEPRECATED'),
    ]);
    const wrapper = mount(VocabularyDetails, {
      props: { name: 'Alpha' },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(wrapper.text()).toContain('Version 3 of Alpha was created.');
    expect(wrapper.find('section[aria-label="Validation warnings"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('This version is deprecated.');
    expect(wrapper.text()).toContain('Latest ACTIVE version: 2');
    expect(wrapper.text()).not.toContain('Update');
    await wrapper
      .findAll('tbody button')
      .find((b) => b.text() === '1')
      ?.trigger('click');
    expect(wrapper.find('section[aria-label="Version 1"]').text()).toContain('ga-admin@example.com');
  });

  it('shows no recommendation for a deprecated vocabulary', async () => {
    useSession(true);
    vi.spyOn(api, 'getVocabulary').mockResolvedValue(vocabulary('DEPRECATED'));
    vi.spyOn(api, 'listAllVersions').mockResolvedValue([version(1, 'ACTIVE')]);
    const wrapper = mount(VocabularyDetails, {
      props: { name: 'Alpha' },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(wrapper.text()).toContain('This vocabulary is deprecated. Its use is discouraged.');
    expect(wrapper.text()).not.toContain('Latest ACTIVE version');
    expect(wrapper.text()).toContain('Activate');
  });

  it('shows not found and load errors', async () => {
    useSession(true);
    vi.spyOn(api, 'getVocabulary').mockRejectedValue(new ApiError({ status: 404 }));
    const notFound = mount(VocabularyDetails, {
      props: { name: 'Gone' },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(notFound.text()).toContain('Vocabulary not found.');
    expect(notFound.text()).toContain('Back to vocabulary list');
    vi.spyOn(api, 'getVocabulary').mockRejectedValue(new ApiError({ status: 500, detail: 'Boom' }));
    const failed = mount(VocabularyDetails, { props: { name: 'Gone' }, global: { plugins: [testRouter()] } });
    await flush();
    expect(failed.text()).toContain('The version history could not be loaded.');
    expect(failed.text()).toContain('Boom');
  });
});
