import { reactive } from 'vue'
import type { ValidationWarning } from './types'

export interface Notice {
  id: number
  kind: 'success' | 'error' | 'warning'
  text: string
  warnings?: ValidationWarning[]
}

let next = 1
export const notices = reactive<Notice[]>([])

export function notify(kind: Notice['kind'], text: string, warnings?: ValidationWarning[]) {
  const n: Notice = { id: next++, kind, text, warnings }
  notices.push(n)
  if (kind === 'success' && (!warnings || warnings.length === 0)) {
    setTimeout(() => dismiss(n.id), 8000)
  }
}

export function dismiss(id: number) {
  const i = notices.findIndex((n) => n.id === id)
  if (i >= 0) notices.splice(i, 1)
}
