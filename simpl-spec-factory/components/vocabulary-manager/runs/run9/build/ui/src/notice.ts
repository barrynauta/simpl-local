import { reactive } from 'vue';
import type { ValidationWarning } from './api';

/** A success notification carried to the next page (D14, D72). */
export const notice = reactive<{ message: string; warnings: ValidationWarning[] }>({
  message: '',
  warnings: [],
});

export function setNotice(message: string, warnings: ValidationWarning[] = []): void {
  notice.message = message;
  notice.warnings = warnings;
}

export function clearNotice(): void {
  notice.message = '';
  notice.warnings = [];
}
