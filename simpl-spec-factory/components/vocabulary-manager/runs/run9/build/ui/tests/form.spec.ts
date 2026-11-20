import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import VocabularyForm from '../src/views/VocabularyForm.vue';
import { api, ApiError } from '../src/api';
import { checkTurtle } from '../src/turtleCheck';
import { flush, testRouter, useSession } from './helpers';

function chooseFile(input: HTMLInputElement, file: File): void {
  Object.defineProperty(input, 'files', { value: [file], configurable: true });
  input.dispatchEvent(new Event('change'));
}

describe('Vocabulary form', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('refuses a user without the admin role', () => {
    useSession(false);
    const wrapper = mount(VocabularyForm, { props: { mode: 'create' }, global: { plugins: [testRouter()] } });
    expect(wrapper.find('[role="alert"]').text()).toBe('You are not authorized to perform this action.');
  });

  it('keeps Submit enabled with field errors, sends nothing and focuses the first error', async () => {
    useSession(true);
    const create = vi.spyOn(api, 'createVocabulary');
    const wrapper = mount(VocabularyForm, {
      props: { mode: 'create' },
      global: { plugins: [testRouter()] },
      attachTo: document.body,
    });
    const submit = wrapper.find('button[type="submit"]');
    expect(submit.attributes('disabled')).toBeUndefined();
    await wrapper.find('form').trigger('submit');
    await flush();
    expect(create).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('Name is required.');
    expect(document.activeElement?.id).toBe('field-name');
    wrapper.unmount();
  });

  it('disables Submit on a syntax error and on a file over 5 MB', async () => {
    useSession(true);
    const wrapper = mount(VocabularyForm, { props: { mode: 'create' }, global: { plugins: [testRouter()] } });
    const input = wrapper.find('#field-file').element as HTMLInputElement;
    chooseFile(input, new File(['@prefix ex: <http://e.org/> .\nex:a ex:b .'], 'bad.ttl'));
    await flush();
    await flush();
    expect(wrapper.text()).toContain('line 2');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined();
    chooseFile(input, new File(['x'.repeat(5 * 1024 * 1024 + 1)], 'big.ttl'));
    await flush();
    expect(wrapper.text()).toContain('The file is larger than 5 MB.');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined();
    chooseFile(input, new File(['@prefix ex: <http://e.org/> .\nex:a ex:b ex:c .'], 'good.ttl'));
    await flush();
    await flush();
    expect(wrapper.text()).toContain('Syntax check passed.');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined();
  });

  it('shows the validation report of a 400 and the 409 detail', async () => {
    useSession(true);
    const create = vi.spyOn(api, 'createVocabulary').mockRejectedValue(
      new ApiError({
        status: 400,
        issues: [{ detail: 'Literal value for object property', location: 'statement x' }],
      }),
    );
    const wrapper = mount(VocabularyForm, { props: { mode: 'create' }, global: { plugins: [testRouter()] } });
    await wrapper.find('#field-name').setValue('Alpha');
    await wrapper.find('#field-description').setValue('About');
    chooseFile(
      wrapper.find('#field-file').element as HTMLInputElement,
      new File(['@prefix ex: <http://e.org/> .\nex:a ex:b ex:c .'], 'good.ttl'),
    );
    await flush();
    await flush();
    await wrapper.find('form').trigger('submit');
    await flush();
    const report = wrapper.find('section[aria-label="Validation report"]');
    expect(report.find('[role="alert"]').text()).toBe('1 errors found');
    expect(report.text()).toContain('Literal value for object property');
    create.mockRejectedValue(
      new ApiError({ status: 409, detail: "Vocabulary with name 'Alpha' already exists." }),
    );
    await wrapper.find('form').trigger('submit');
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toContain('already exists');
  });

  it('pre-fills the new version form from the latest version', async () => {
    useSession(true);
    vi.spyOn(api, 'getVocabulary').mockResolvedValue({
      href: '',
      contentHref: '',
      name: 'Alpha',
      version: 4,
      description: 'Latest description',
      status: 'ACTIVE',
      createdBy: 'x',
      creationDate: '',
      lastUpdateDate: '',
    });
    vi.spyOn(api, 'content').mockResolvedValue(new Blob(['x']));
    const wrapper = mount(VocabularyForm, {
      props: { mode: 'version', name: 'Alpha' },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect((wrapper.find('#field-description').element as HTMLTextAreaElement).value).toBe(
      'Latest description',
    );
    expect(wrapper.text()).toContain('Alpha.ttl (current content)');
    expect(wrapper.text()).toContain(
      'Submitting creates version 5 and deprecates every older ACTIVE version.',
    );
  });

  it('checks Turtle in the browser', () => {
    expect(checkTurtle('@prefix ex: <http://e.org/> . ex:a ex:b ex:c .').ok).toBe(true);
    expect(checkTurtle('ex:a ex:b ex:c .').message).toContain('line 1');
  });
});
