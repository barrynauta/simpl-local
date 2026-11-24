import { flushPromises } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ACTIVE, mountAt, reply, signIn, stubFetch } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('New version form', () => {
  it('pre-fills the description, attaches the current content and shows the next number', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    stubFetch((_m, path) =>
      path.startsWith('/v1/content/')
        ? reply(200, '@prefix ex: <http://example.org/> .', 'text/turtle')
        : reply(200, ACTIVE),
    );
    const wrapper = await mountAt('/vocabularies/Health/versions/new');
    expect((wrapper.find('#description').element as HTMLTextAreaElement).value).toBe('Health terms');
    expect(wrapper.text()).toContain('Health.ttl (current content)');
    expect(wrapper.text()).toContain(
      'Submitting creates version 3 and deprecates every older ACTIVE version.',
    );
  });

  it('shows the validation report of a 400 and keeps the form', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    stubFetch((method, path) => {
      if (method === 'POST') {
        return reply(400, {
          status: 400,
          detail: 'The input message is incorrect',
          issues: [
            { name: 'vocabularyFile', detail: 'Literal value for object property', location: 'statement x' },
            { name: 'vocabularyFile', detail: 'Vocabulary contains no statements', location: 'document' },
          ],
        });
      }
      return path.startsWith('/v1/content/') ? reply(200, 'x', 'text/turtle') : reply(200, ACTIVE);
    });
    const wrapper = await mountAt('/vocabularies/Health/versions/new');
    await wrapper.find('#changelog').setValue('Changed');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const report = wrapper.find('section[aria-label="Validation report"]');
    expect(report.find('[role="alert"]').text()).toBe('2 errors found');
    expect(wrapper.find('form').exists()).toBe(true);
  });
});
