import { flushPromises } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { checkTurtle } from '../src/lib/turtle';
import { mountAt, reply, signIn, stubFetch } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('Create form', () => {
  it('refuses users without the admin role', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    stubFetch(() => reply(200, {}));
    const wrapper = await mountAt('/vocabularies/new');
    expect(wrapper.find('[role="alert"]').text()).toBe('You are not authorized to perform this action.');
  });

  it('checks every field on submit, sends nothing and focuses the first error', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    const fetchMock = stubFetch(() => reply(200, {}));
    const wrapper = await mountAt('/vocabularies/new');
    expect(wrapper.find('label[for="name"]').text()).toContain('Name');
    expect(wrapper.find('#name').attributes('aria-required')).toBe('true');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.find('#name-error').text()).not.toBe('');
    expect(wrapper.find('#description-error').text()).not.toBe('');
    expect(wrapper.find('#vocabulary-file-message').text()).toBe('Vocabulary file is required.');
    expect(fetchMock).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe('name');
  });

  it('checks Turtle syntax in the browser', () => {
    expect(checkTurtle('@prefix ex: <http://example.org/> . ex:a ex:b ex:c .').ok).toBe(true);
    const bad = checkTurtle('@prefix ex: <http://example.org/> .\nex:a ex:b .');
    expect(bad.ok).toBe(false);
    expect(bad.line).toBe(2);
  });
});
