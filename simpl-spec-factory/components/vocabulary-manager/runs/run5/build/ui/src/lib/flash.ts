import { ref } from 'vue';
import type { ValidationWarning } from './api';

/** A notification carried from one screen to the next (D14). */
export interface Flash {
  message: string;
  warnings: ValidationWarning[];
}

export const flash = ref<Flash | null>(null);

export function takeFlash(): Flash | null {
  const value = flash.value;
  flash.value = null;
  return value;
}
