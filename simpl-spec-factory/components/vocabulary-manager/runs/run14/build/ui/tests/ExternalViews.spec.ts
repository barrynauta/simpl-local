import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountAt, page, reply, signIn, stubFetch } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

const DC = {
  href: '/external-vocabularies/DublinCore',
  name: 'DublinCore',
  namespaceUri: 'http://purl.org/dc/terms/',
  aliasUris: ['https://purl.org/dc/terms/'],
  description: 'DCMI terms',
  createdBy: 'ga-admin@example.com',
  creationDate: '2026-11-18T09:27:00Z',
};

describe('External vocabularies', () => {
  it('lists external vocabularies for a viewer without register or remove', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    stubFetch(() => reply(200, page([DC])));
    const wrapper = await mountAt('/external-vocabularies');
    expect(wrapper.findAll('th').map((th) => th.text())).toEqual([
      'Name',
      'Namespace URI',
      'Alias URIs',
      'Description',
      'Created by',
      'Date created',
      'Actions',
    ]);
    expect(wrapper.text()).toContain('Download');
    expect(wrapper.text()).not.toContain('Remove');
    expect(wrapper.text()).not.toContain('Register external vocabulary +');
  });

  it('shows the register form to an admin', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    stubFetch(() => reply(200, page([])));
    const wrapper = await mountAt('/external-vocabularies/new');
    expect(wrapper.find('label[for="namespace-uri"]').text()).toContain('Namespace URI');
    expect(wrapper.find('label[for="alias-uris"]').text()).toBe('Alias URIs');
  });
});
