import { reactive } from 'vue'
import type { ValidationWarning } from './api'

/** Notification carried from a form to the details page after creation (D14, D72). */
export const flash = reactive<{ message: string; warnings: ValidationWarning[] }>({
  message: '',
  warnings: [],
})

export function setFlash(message: string, warnings: ValidationWarning[]): void {
  flash.message = message
  flash.warnings = warnings
}

export function takeFlash(): { message: string; warnings: ValidationWarning[] } {
  const value = { message: flash.message, warnings: [...flash.warnings] }
  flash.message = ''
  flash.warnings = []
  return value
}
