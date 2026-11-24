import { afterEach, describe, expect, it, vi } from 'vitest';
import { ACTIVE, mountAt, page, reply, signIn, stubFetch, version } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

describe('Vocabulary details', () => {
  it('shows the history with the deprecated warning and the latest ACTIVE link', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    stubFetch((_method, path) =>
      path.includes('/versions')
        ? reply(200, page([version(2, 'ACTIVE'), version(1, 'DEPRECATED')]))
        : reply(200, ACTIVE),
    );
    const wrapper = await mountAt('/vocabularies/Health/versions/1');
    expect(wrapper.find('h1').text()).toBe('Health');
    const panel = wrapper.find('section[aria-label="Version 1"]');
    expect(panel.exists()).toBe(true);
    expect(panel.text()).toContain('This version is deprecated.');
    expect(panel.find('a').text()).toBe('Latest ACTIVE version: 2');
    expect(wrapper.find('tr[aria-current="true"]').text()).toContain('Change 1');
    expect(wrapper.text()).not.toContain('Update');
  });

  it('shows no link when the vocabulary is deprecated', async () => {
    signIn(['GA_VOCABULARY_ADMIN']);
    stubFetch((_method, path) =>
      path.includes('/versions')
        ? reply(200, page([version(2, 'ACTIVE'), version(1, 'DEPRECATED')]))
        : reply(200, { ...ACTIVE, status: 'DEPRECATED' }),
    );
    const wrapper = await mountAt('/vocabularies/Health/versions/2');
    expect(wrapper.find('p[role="status"]').text()).toBe(
      'This vocabulary is deprecated. Its use is discouraged.',
    );
    const panel = wrapper.find('section[aria-label="Version 2"]');
    expect(panel.text()).toContain('This version is deprecated.');
    expect(panel.text()).not.toContain('Latest ACTIVE version');
  });

  it('shows not found and load errors', async () => {
    signIn(['GA_VOCABULARY_VIEWER']);
    stubFetch(() => reply(404, { status: 404, detail: "Vocabulary with name 'Nope' was not found." }));
    let wrapper = await mountAt('/vocabularies/Nope');
    expect(wrapper.find('[role="alert"]').text()).toBe('Vocabulary not found.');
    expect(wrapper.text()).toContain('Back to vocabulary list');
    wrapper.unmount();
    stubFetch(() =>
      reply(503, { status: 503, detail: 'The vocabulary store is not available. Retry later.' }),
    );
    wrapper = await mountAt('/vocabularies/Health');
    expect(wrapper.find('[role="alert"]').text()).toContain('The version history could not be loaded.');
  });
});
