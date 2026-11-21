import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { apiMock, resetApiMock } from './mockApi';
import { mountAt, signIn } from './helpers';
import { flash } from '../src/flash';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  api: (await import('./mockApi')).apiMock,
}));

const { ApiError } = await import('../src/api');

async function chooseFile(input: HTMLInputElement, content: string, name = 'v.ttl') {
  const file = new File([content], name, { type: 'text/turtle' });
  Object.defineProperty(input, 'files', { value: [file], configurable: true });
  input.dispatchEvent(new Event('change'));
  await flushPromises();
  await new Promise((r) => setTimeout(r, 0));
  await flushPromises();
}

const VALID = '@prefix ex: <http://example.org/> .\nex:a ex:b "c" .\n';

describe('Create vocabulary form', () => {
  beforeEach(() => {
    resetApiMock();
    document.body.innerHTML = '';
  });

  it('shows the not-authorized message to a viewer (D18)', async () => {
    signIn(false);
    const { wrapper } = await mountAt('/vocabularies/new');
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'You are not authorized to perform this action.',
    );
    expect(wrapper.find('form').exists()).toBe(false);
  });

  it('has the decided fields and no change log (D32, D60, D141)', async () => {
    signIn(true);
    const { wrapper } = await mountAt('/vocabularies/new');
    const labels = wrapper.findAll('label').map((l) => l.text());
    expect(labels).toEqual(['Name', 'Description', 'Vocabulary file']);
    expect(wrapper.find('#name').attributes('aria-required')).toBe('true');
    expect(wrapper.text()).not.toContain('Change log');
  });

  it('checks every field on Submit and sends nothing (D101)', async () => {
    signIn(true);
    const { wrapper } = await mountAt('/vocabularies/new');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.text()).toContain('Name is required.');
    expect(wrapper.text()).toContain('Description is required.');
    expect(wrapper.text()).toContain('Vocabulary file is required.');
    expect(apiMock.createVocabulary).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe('name');
  });

  it('disables Submit on a syntax error and enables it after a valid file (D13, D62)', async () => {
    signIn(true);
    const { wrapper } = await mountAt('/vocabularies/new');
    const input = wrapper.find('#vocabularyFile').element as HTMLInputElement;
    await chooseFile(input, '@prefix ex: <http://example.org/> .\nex:a ex:b ;; .\n');
    expect(wrapper.text()).toContain('Syntax error at line 2');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined();
    await chooseFile(input, VALID);
    expect(wrapper.text()).toContain('Syntax check passed.');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined();
  });

  it('refuses a file over 5 MB (D76)', async () => {
    signIn(true);
    const { wrapper } = await mountAt('/vocabularies/new');
    const input = wrapper.find('#vocabularyFile').element as HTMLInputElement;
    const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'big.ttl');
    Object.defineProperty(input, 'files', { value: [big], configurable: true });
    input.dispatchEvent(new Event('change'));
    await flushPromises();
    expect(wrapper.text()).toContain('The file is larger than 5 MB.');
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined();
  });

  it('shows the validation report on 400 (D14, D90)', async () => {
    signIn(true);
    apiMock.createVocabulary.mockRejectedValue(
      new ApiError(400, {
        status: 400,
        issues: [
          { detail: 'Literal value for object property', location: "statement 'a b c'" },
          { detail: 'Vocabulary contains no statements', location: 'document' },
        ],
      }),
    );
    const { wrapper } = await mountAt('/vocabularies/new');
    await wrapper.find('#name').setValue('Health');
    await wrapper.find('#description').setValue('Desc');
    await chooseFile(wrapper.find('#vocabularyFile').element as HTMLInputElement, VALID);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const report = wrapper.find('section[aria-label="Validation report"]');
    expect(report.find('[role="alert"]').text()).toBe('2 errors found');
    expect(report.text()).toContain('Literal value for object property');
    const sent = apiMock.createVocabulary.mock.calls[0][0] as FormData;
    expect(sent.get('changelog')).toBe('Initial version.');
  });

  it('shows the 409 detail and opens the details page after 201 (D72, D161)', async () => {
    signIn(true);
    apiMock.createVocabulary.mockRejectedValueOnce(
      new ApiError(409, { detail: "Vocabulary with name 'Health' already exists." }),
    );
    const { wrapper, router } = await mountAt('/vocabularies/new');
    await wrapper.find('#name').setValue('Health');
    await wrapper.find('#description').setValue('Desc');
    await chooseFile(wrapper.find('#vocabularyFile').element as HTMLInputElement, VALID);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.find('[role="alert"]').text()).toContain(
      "Vocabulary with name 'Health' already exists.",
    );
    apiMock.createVocabulary.mockResolvedValueOnce({
      name: 'Health',
      version: 1,
      validationWarnings: [],
    });
    apiMock.getVocabulary.mockResolvedValue({ name: 'Health' });
    apiMock.listAllVersions.mockResolvedValue([]);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(router.currentRoute.value.fullPath).toBe('/vocabularies/Health');
    expect(flash.message === '' || flash.message === 'Version 1 of Health was created.').toBe(true);
  });
});
