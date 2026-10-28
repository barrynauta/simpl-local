import { reactive } from 'vue';

/** One notification slot shared by all pages, so it survives a redirect. */
export const notification = reactive<{ kind: 'success' | 'error' | null; text: string }>({ kind: null, text: '' });

let timer: number | undefined;

function show(kind: 'success' | 'error', text: string): void {
  notification.kind = kind;
  notification.text = text;
  window.clearTimeout(timer);
  timer = window.setTimeout(clear, 15000);
}

export const notifySuccess = (text: string): void => show('success', text);
export const notifyError = (text: string): void => show('error', text);

export function clear(): void {
  notification.kind = null;
  notification.text = '';
}
