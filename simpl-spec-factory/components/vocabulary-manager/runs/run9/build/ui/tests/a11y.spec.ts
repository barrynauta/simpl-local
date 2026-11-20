import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import axe from 'axe-core';
import VocabularyList from '../src/views/VocabularyList.vue';
import VocabularyDetails from '../src/views/VocabularyDetails.vue';
import VocabularyForm from '../src/views/VocabularyForm.vue';
import { api } from '../src/api';
import { flush, testRouter, useSession } from './helpers';

/** Q6: no axe violations on the list, details and both form pages (jsdom: no colour contrast check). */
async function violations(element: Element): Promise<string[]> {
  const result = await axe.run(element, { rules: { 'color-contrast': { enabled: false } } });
  return result.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.html).join(' | ')}`);
}

const vocabulary = {
  href: '',
  contentHref: '',
  name: 'Alpha',
  version: 1,
  description: 'About',
  status: 'DEPRECATED' as const,
  createdBy: 'x',
  creationDate: '2026-11-18T09:27:00Z',
  lastUpdateDate: '2026-11-18T09:27:00Z',
};

describe('Accessibility', () => {
  it('has no axe violations on the four pages', async () => {
    useSession(true);
    vi.spyOn(api, 'listVocabularies').mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [vocabulary],
    });
    vi.spyOn(api, 'getVocabulary').mockResolvedValue(vocabulary);
    vi.spyOn(api, 'listAllVersions').mockResolvedValue([
      { ...vocabulary, changelog: 'Initial version.', status: 'DEPRECATED' },
    ]);
    vi.spyOn(api, 'content').mockResolvedValue(new Blob(['x']));
    const pages: [unknown, Record<string, unknown>][] = [
      [VocabularyList, {}],
      [VocabularyDetails, { name: 'Alpha' }],
      [VocabularyForm, { mode: 'create' }],
      [VocabularyForm, { mode: 'version', name: 'Alpha' }],
    ];
    for (const [component, props] of pages) {
      const main = document.createElement('main');
      const host = document.createElement('div');
      main.appendChild(host);
      document.body.appendChild(main);
      const wrapper = mount(component as typeof VocabularyList, {
        props,
        global: { plugins: [testRouter()] },
        attachTo: host,
      });
      await flush();
      await flush();
      expect(await violations(main)).toEqual([]);
      wrapper.unmount();
      main.remove();
    }
  });
});
