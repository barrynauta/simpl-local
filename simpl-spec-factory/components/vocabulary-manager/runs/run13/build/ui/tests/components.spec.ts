import { afterEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import FileField from '../src/components/FileField.vue';
import App from '../src/App.vue';
import { isAdmin, loadConfig } from '../src/auth';
import { takeNotice, pendingNotice } from '../src/messages';
import { asUser, mountAt } from './helpers';

afterEach(() => {
  vi.unstubAllGlobals();
  document.body.innerHTML = '';
});

function choose(wrapper: ReturnType<typeof mount>, file: File | null) {
  const input = wrapper.find('input[type="file"]');
  Object.defineProperty(input.element, 'files', { value: file ? [file] : [], configurable: true });
  return input.trigger('change');
}

describe('File field', () => {
  it('reports a passed syntax check', async () => {
    const wrapper = mount(FileField, { props: { submitError: '' } });
    await choose(wrapper, new File(['<http://a> <http://b> <http://c> .'], 'ok.ttl'));
    await flushPromises();
    expect(wrapper.find('[role="status"]').text()).toBe('Syntax check passed.');
    expect(wrapper.emitted('change')?.[0][1]).toBe(false);
  });

  it('reports a syntax error with its line and blocks submit', async () => {
    const wrapper = mount(FileField, { props: { submitError: '' } });
    await choose(wrapper, new File(['<http://a> <http://b> .\n'], 'bad.ttl'));
    await flushPromises();
    expect(wrapper.find('#field-file-error').text()).toMatch(/^Line 1/);
    expect(wrapper.emitted('change')?.[0][1]).toBe(true);
  });

  it('refuses a file over 5 MB with the decided text', async () => {
    const wrapper = mount(FileField, { props: { submitError: '' } });
    await choose(wrapper, new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'big.ttl'));
    await flushPromises();
    expect(wrapper.find('#field-file-error').text()).toBe('The file is larger than 5 MB.');
  });

  it('falls back to the current content label when the choice is cleared', async () => {
    const wrapper = mount(FileField, {
      props: { submitError: '', currentLabel: 'Health.ttl (current content)' },
    });
    await choose(wrapper, null);
    expect(wrapper.text()).toContain('Health.ttl (current content)');
    expect(wrapper.emitted('change')?.[0]).toEqual([null, false]);
  });
});

describe('App shell and auth', () => {
  it('renders the menu item and the user', async () => {
    asUser(true);
    const { wrapper } = await mountAt('/vocabularies/new', App);
    expect(wrapper.find('nav a').text()).toBe('Vocabularies');
    expect(wrapper.text()).toContain('tester');
    expect(isAdmin()).toBe(true);
  });

  it('loads the runtime configuration', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('{"keycloakUrl":"http://kc","realm":"simpl","clientId":"c"}')),
    );
    expect((await loadConfig()).realm).toBe('simpl');
  });

  it('hands a notice over once', () => {
    pendingNotice.value = { message: 'm', warnings: [] };
    expect(takeNotice()?.message).toBe('m');
    expect(takeNotice()).toBeNull();
  });
});
