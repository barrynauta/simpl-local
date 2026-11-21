import { afterEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import CreateVocabulary from '../src/views/CreateVocabulary.vue';
import { pendingNotice } from '../src/messages';
import { asUser, jsonResponse, mockFetch, mountAt, version } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

const ttl = () =>
  new File(['<http://a> <http://b> <http://c> .'], 'a.ttl', { type: 'text/turtle' });

async function fill(wrapper: Awaited<ReturnType<typeof mountAt>>['wrapper']) {
  await wrapper.find('#field-name').setValue('Health');
  await wrapper.find('#field-description').setValue('Health terms');
  const vm = wrapper.vm as unknown as { onFile: (f: File, b: boolean) => void };
  vm.onFile(ttl(), false);
}

describe('Create vocabulary form', () => {
  it('tells a viewer that the action is not allowed', async () => {
    asUser(false);
    mockFetch(() => jsonResponse({}));
    const { wrapper } = await mountAt('/vocabularies/new', CreateVocabulary);
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'You are not authorized to perform this action.',
    );
    expect(wrapper.find('form').exists()).toBe(false);
  });

  it('has the decided fields and no version or change log field', async () => {
    asUser(true);
    mockFetch(() => jsonResponse({}));
    const { wrapper } = await mountAt('/vocabularies/new', CreateVocabulary);
    const labels = wrapper.findAll('label').map((l) => l.text().replace('*', '').trim());
    expect(labels).toEqual(['Name', 'Description', 'Vocabulary file']);
    expect(wrapper.find('#field-name').attributes('aria-required')).toBe('true');
    expect(wrapper.text()).not.toContain('Change log');
  });

  it('checks every field on Submit, sends nothing and focuses the first error', async () => {
    asUser(true);
    const calls = mockFetch(() => jsonResponse({}));
    const { wrapper } = await mountAt('/vocabularies/new', CreateVocabulary);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(calls).toHaveLength(0);
    expect(wrapper.find('#field-name-error').text()).toBe('Name is required.');
    expect(wrapper.find('#field-description-error').text()).toBe('Description is required.');
    expect(wrapper.find('#field-file-error').text()).toBe('Vocabulary file is required.');
    expect(document.activeElement?.id).toBe('field-name');
  });

  it('shows the validation report on a 400', async () => {
    asUser(true);
    mockFetch(() =>
      jsonResponse(
        {
          status: 400,
          issues: [
            { detail: 'Literal value for object property', location: "statement 'a b c'" },
            { detail: 'Vocabulary contains no statements', location: 'document' },
          ],
        },
        400,
      ),
    );
    const { wrapper } = await mountAt('/vocabularies/new', CreateVocabulary);
    await fill(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const report = wrapper.find('section[aria-label="Validation report"]');
    expect(report.find('[role="alert"]').text()).toBe('2 errors found');
    expect(report.text()).toContain('Literal value for object property');
  });

  it('shows the API detail of a 409', async () => {
    asUser(true);
    mockFetch(() =>
      jsonResponse({ status: 409, detail: "Vocabulary with name 'Health' already exists." }, 409),
    );
    const { wrapper } = await mountAt('/vocabularies/new', CreateVocabulary);
    await fill(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.find('[role="alert"]').text()).toBe(
      "Vocabulary with name 'Health' already exists.",
    );
    expect((wrapper.find('#field-name').element as HTMLInputElement).value).toBe('Health');
  });

  it('sends the form with the initial changelog and opens the details page', async () => {
    asUser(true);
    const calls = mockFetch(() =>
      jsonResponse({ ...version('Health', 1), validationWarnings: [] }, 201),
    );
    const { wrapper, router } = await mountAt('/vocabularies/new', CreateVocabulary);
    await fill(wrapper);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const body = calls[0].init.body as FormData;
    expect(calls[0].url).toBe('/v1/vocabularies');
    expect(body.get('changelog')).toBe('Initial version.');
    expect(body.get('name')).toBe('Health');
    expect(router.currentRoute.value.fullPath).toBe('/vocabularies/Health');
    expect(pendingNotice.value?.message).toBe('Version 1 of Health was created.');
  });
});
