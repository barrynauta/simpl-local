import { reactive } from 'vue';
import type { ValidationWarning } from './api';

/** The success notification carried from a form to the details page (D14, D72, D113). */
export interface CreationNotice {
  name: string;
  version: number;
  warnings: ValidationWarning[];
}

export const notice = reactive<{ value: CreationNotice | null }>({ value: null });

export function setNotice(value: CreationNotice): void {
  notice.value = value;
}

export function takeNotice(name: string): CreationNotice | null {
  const value = notice.value;
  if (value && value.name === name) {
    notice.value = null;
    return value;
  }
  return null;
}
