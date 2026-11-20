import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import CreateVocabulary from '../src/views/CreateVocabulary.vue';
import NewVersion from '../src/views/NewVersion.vue';
import * as api from '../src/api';
import { checkTurtle, syntaxErrorText } from '../src/turtleCheck';
import { flush, routerAt, signInAs } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  createVocabulary: vi.fn(),
  createVersion: vi.fn(),
  getVocabulary: vi.fn(),
  latestContent: vi.fn(),
}));

describe('CreateVocabulary', () => {
  it('refuses a user without the admin role', async () => {
    signInAs(['GA_VOCABULARY_VIEWER']);
    const wrapper = mount(CreateVocabulary, { global: { plugins: [await routerAt('/vocabularies/new')] } });
    expect(wrapper.find('[role="alert"]').text()).toBe('You are not authorized to perform this action.');
    expect(wrapper.find('form').exists()).toBe(false);
  });

  it('checks every field on submit, sends nothing and has no change log field', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    const wrapper = mount(CreateVocabulary, {
      attachTo: document.body,
      global: { plugins: [await routerAt('/vocabularies/new')] },
    });
    expect(wrapper.find('#field-changelog').exists()).toBe(false);
    await wrapper.find('#field-name').setValue('bad');
    await wrapper.find('form').trigger('submit');
    await flush();
    expect(wrapper.find('#error-name').text()).toBe(
      'Name must start with an upper-case letter and contain only letters and digits.',
    );
    expect(wrapper.find('#error-description').text()).toBe('Description is required.');
    expect(wrapper.find('#error-file').text()).toBe('Vocabulary file is required.');
    expect(document.activeElement?.id).toBe('field-name');
    expect(api.createVocabulary).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('shows the validation report of a 400', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    vi.mocked(api.createVocabulary).mockRejectedValue(
      new api.ApiError(400, {
        issues: [
          { detail: 'Literal value for object property', location: "statement 'a b c'" },
          { detail: 'Vocabulary contains no statements', location: 'document' },
        ],
      }),
    );
    const wrapper = mount(CreateVocabulary, { global: { plugins: [await routerAt('/vocabularies/new')] } });
    await wrapper.find('#field-name').setValue('Health');
    await wrapper.find('#field-description').setValue('Desc');
    const input = wrapper.find('#field-file').element as HTMLInputElement;
    const file = new File(['@prefix ex: <http://e/> . ex:a ex:b ex:c .'], 'h.ttl', { type: 'text/turtle' });
    Object.defineProperty(input, 'files', { value: [file] });
    await wrapper.find('#field-file').trigger('change');
    await flush();
    await flush();
    expect(wrapper.text()).toContain('Syntax check passed.');
    await wrapper.find('form').trigger('submit');
    await flush();
    const region = wrapper.find('section[aria-label="Validation report"]');
    expect(region.find('[role="alert"]').text()).toBe('2 errors found');
    expect(region.text()).toContain('Literal value for object property');
  });
});

describe('NewVersion', () => {
  it('pre-fills the description and the current content', async () => {
    signInAs(['GA_VOCABULARY_ADMIN']);
    vi.mocked(api.getVocabulary).mockResolvedValue({
      href: '',
      contentHref: '',
      name: 'Health',
      version: 2,
      description: 'Latest description',
      status: 'ACTIVE',
      createdBy: 'x',
      creationDate: '',
      lastUpdateDate: '',
    });
    vi.mocked(api.latestContent).mockResolvedValue(new Blob(['x']));
    const wrapper = mount(NewVersion, {
      props: { name: 'Health' },
      global: { plugins: [await routerAt('/vocabularies/Health/versions/new')] },
    });
    await flush();
    expect((wrapper.find('#field-description').element as HTMLTextAreaElement).value).toBe(
      'Latest description',
    );
    expect(wrapper.text()).toContain('Health.ttl (current content)');
    expect(wrapper.text()).toContain(
      'Submitting creates version 3 and deprecates every older ACTIVE version.',
    );
    expect(wrapper.find('label[for="field-changelog"]').text()).toBe('Change log');
  });
});

describe('checkTurtle', () => {
  it('gives the line of the first syntax error', () => {
    expect(checkTurtle('@prefix ex: <http://e/> .\nex:a ex:b ex:c .').ok).toBe(true);
    const result = checkTurtle('@prefix ex: <http://e/> .\nex:a ex:b .\n');
    expect(result.ok).toBe(false);
    expect(result.line).toBe(2);
    expect(syntaxErrorText(result)).toContain('line 2');
  });
});
