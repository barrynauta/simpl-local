import { reactive } from 'vue';
import type { ValidationWarning } from './api';

/** The D14 notification carried from a form to the details page (D72, D113). */
export const flash = reactive({
  name: '',
  message: '',
  warnings: [] as ValidationWarning[],
});

export function setFlash(name: string, message: string, warnings: ValidationWarning[]): void {
  flash.name = name;
  flash.message = message;
  flash.warnings = [...warnings];
}

export function takeFlash(name: string): { message: string; warnings: ValidationWarning[] } | null {
  if (!flash.message || flash.name !== name) return null;
  const result = { message: flash.message, warnings: [...flash.warnings] };
  flash.name = '';
  flash.message = '';
  flash.warnings = [];
  return result;
}
