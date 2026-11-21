import { afterEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import NewVersion from '../src/views/NewVersion.vue';
import { asUser, jsonResponse, mockFetch, mountAt, vocab, version } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('New version form', () => {
  it('prefills the description, names the current file and states the effect', async () => {
    asUser(true);
    mockFetch(() => jsonResponse(vocab('Health', 'ACTIVE', 3)));
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new', NewVersion);
    expect((wrapper.find('#field-description').element as HTMLTextAreaElement).value).toBe(
      'Description of Health',
    );
    expect(wrapper.text()).toContain('Health.ttl (current content)');
    expect(wrapper.text()).toContain(
      'Submitting creates version 4 and deprecates every older ACTIVE version.',
    );
    expect((wrapper.find('#field-changelog').element as HTMLTextAreaElement).value).toBe('');
  });

  it('requires a change log', async () => {
    asUser(true);
    const calls = mockFetch(() => jsonResponse(vocab('Health', 'ACTIVE', 3)));
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new', NewVersion);
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(calls).toHaveLength(1);
    expect(wrapper.find('#field-changelog-error').text()).toBe('Change log is required.');
  });

  it('sends the current content when no file is chosen and opens the new version', async () => {
    asUser(true);
    const calls = mockFetch((url, init) => {
      if (url.startsWith('/v1/content/'))
        return new Response('<http://a> <http://b> <http://c> .', {
          headers: { 'Content-Disposition': 'attachment; filename="Health.ttl"' },
        });
      if (init.method === 'POST')
        return jsonResponse({ ...version('Health', 4), validationWarnings: [] }, 201);
      return jsonResponse(vocab('Health', 'ACTIVE', 3));
    });
    const { wrapper, router } = await mountAt('/vocabularies/Health/versions/new', NewVersion);
    await wrapper.find('#field-changelog').setValue('Only the description changed.');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const post = calls.find((c) => c.init.method === 'POST');
    expect(post?.url).toBe('/v1/vocabularies/Health/versions');
    const body = post?.init.body as FormData;
    expect(body.get('version')).toBeNull();
    expect((body.get('vocabularyFile') as File).name).toBe('Health.ttl');
    expect(router.currentRoute.value.fullPath).toBe('/vocabularies/Health/versions/4');
  });

  it('shows the generic error with the detail on a 500', async () => {
    asUser(true);
    mockFetch((url, init) =>
      init.method === 'POST'
        ? jsonResponse({ status: 500, detail: 'Internal Server Error' }, 500)
        : jsonResponse(vocab('Health', 'ACTIVE', 3)),
    );
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new', NewVersion);
    const vm = wrapper.vm as unknown as { onFile: (f: File, b: boolean) => void };
    vm.onFile(new File(['<http://a> <http://b> <http://c> .'], 'n.ttl'), false);
    await wrapper.find('#field-changelog').setValue('x');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'Something went wrong. Retry later or contact support. Internal Server Error',
    );
  });

  it('tells a viewer that the action is not allowed', async () => {
    asUser(false);
    mockFetch(() => jsonResponse({}));
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new', NewVersion);
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'You are not authorized to perform this action.',
    );
  });
});
