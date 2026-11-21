import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { apiMock, resetApiMock } from './mockApi';
import { mountAt, signIn, vocab } from './helpers';

vi.mock('../src/api', async (original) => ({
  ...(await original<typeof import('../src/api')>()),
  api: (await import('./mockApi')).apiMock,
}));

const { ApiError } = await import('../src/api');

describe('New version form', () => {
  beforeEach(() => {
    resetApiMock();
    document.body.innerHTML = '';
  });

  it('is pre-filled and states what Submit does (D33, D46)', async () => {
    signIn(true);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health', 'ACTIVE', 3));
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new');
    expect((wrapper.find('#description').element as HTMLTextAreaElement).value).toBe(
      'Description of Health',
    );
    expect((wrapper.find('#changelog').element as HTMLTextAreaElement).value).toBe('');
    expect(wrapper.text()).toContain('Health.ttl (current content)');
    expect(wrapper.text().replace(/\s+/g, ' ')).toContain(
      'Submitting creates version 4 and deprecates every older ACTIVE version.',
    );
  });

  it('requires a change log and sends the current content without a new file (D33, D37, D113)', async () => {
    signIn(true);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health', 'ACTIVE', 3));
    apiMock.downloadContent.mockResolvedValue({
      blob: new Blob(['@prefix ex: <http://example.org/> .']),
      filename: 'Health.ttl',
    });
    apiMock.createVersion.mockResolvedValue({ name: 'Health', version: 4, validationWarnings: [] });
    apiMock.listAllVersions.mockResolvedValue([]);
    const { wrapper, router } = await mountAt('/vocabularies/Health/versions/new');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    expect(wrapper.text()).toContain('Change log is required.');
    expect(apiMock.createVersion).not.toHaveBeenCalled();
    await wrapper.find('#changelog').setValue('Description only.');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const sent = apiMock.createVersion.mock.calls[0][1] as FormData;
    expect(sent.get('changelog')).toBe('Description only.');
    expect((sent.get('vocabularyFile') as File).name).toBe('Health.ttl');
    expect(router.currentRoute.value.fullPath).toBe('/vocabularies/Health/versions/4');
  });

  it('shows the generic failure text on 500 (SIMPL-18172 AC3, D60)', async () => {
    signIn(true);
    apiMock.getVocabulary.mockResolvedValue(vocab('Health', 'ACTIVE', 1));
    apiMock.downloadContent.mockResolvedValue({ blob: new Blob(['']), filename: 'Health.ttl' });
    apiMock.createVersion.mockRejectedValue(new ApiError(500, { detail: 'Internal Server Error' }));
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new');
    await wrapper.find('#changelog').setValue('Change.');
    await wrapper.find('form').trigger('submit');
    await flushPromises();
    const alert = wrapper.find('[role="alert"]');
    expect(alert.text()).toContain('Something went wrong. Retry later or contact support.');
    expect(alert.text()).toContain('Internal Server Error');
  });

  it('shows the not-authorized message to a viewer (D18)', async () => {
    signIn(false);
    const { wrapper } = await mountAt('/vocabularies/Health/versions/new');
    expect(wrapper.find('[role="alert"]').text()).toBe(
      'You are not authorized to perform this action.',
    );
    expect(apiMock.getVocabulary).not.toHaveBeenCalled();
  });
});
