import { reactive } from 'vue';
import type { ValidationWarning } from './api';

/** A message shown after navigation: the D14 notification and warnings (D72, D113, D180). */
export interface Notice {
  success?: string;
  warnings?: ValidationWarning[];
}

export const notice = reactive<Notice>({});

export function setNotice(value: Notice): void {
  notice.success = value.success;
  notice.warnings = value.warnings;
}

export function takeNotice(): Notice {
  const value = { success: notice.success, warnings: notice.warnings };
  notice.success = undefined;
  notice.warnings = undefined;
  return value;
}
