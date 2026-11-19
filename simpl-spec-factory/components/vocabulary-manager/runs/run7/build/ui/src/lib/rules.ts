import { Parser } from 'n3'

export const MAX_FILE_BYTES = 5 * 1024 * 1024
const NAME = /^[A-Z][a-zA-Z0-9]*$/

/** Field rules of D37; messages are free text. */
export function nameError(value: string): string {
  if (!value) return 'Name is required.'
  if (value.length < 3 || value.length > 64) return 'Name must be 3 to 64 characters.'
  if (!NAME.test(value))
    return 'Name must start with an upper-case letter and contain only letters and digits.'
  return ''
}

export function textError(label: string, value: string): string {
  const length = [...value.trim()].length
  if (length === 0) return `${label} is required.`
  if (length > 1000) return `${label} must be at most 1000 characters.`
  return ''
}

export interface SyntaxResult {
  ok: boolean
  message: string
}

/** Browser syntax check with the N3.js Turtle parser (D13): line always, column when given (D60). */
export function checkTurtle(text: string, baseIRI: string): SyntaxResult {
  try {
    new Parser({ format: 'text/turtle', baseIRI }).parse(text)
    return { ok: true, message: '' }
  } catch (error) {
    const e = error as Error & { context?: { line?: number; column?: number } }
    const line = e.context?.line
    const column = e.context?.column
    let where = ''
    if (line !== undefined) {
      where = column !== undefined ? ` (line ${line}, column ${column})` : ` (line ${line})`
    }
    const message = (e.message || 'Invalid Turtle').replace(/\s+on line \d+\.?$/, '')
    return { ok: false, message: `Syntax error${where}: ${message}` }
  }
}
