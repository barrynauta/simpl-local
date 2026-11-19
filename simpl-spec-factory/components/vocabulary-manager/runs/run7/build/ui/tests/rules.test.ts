import { describe, expect, it } from 'vitest'
import { formatDate, statusLabel } from '../src/lib/format'
import { checkTurtle, nameError, textError } from '../src/lib/rules'

describe('field rules (D37)', () => {
  it('checks names', () => {
    expect(nameError('')).toBe('Name is required.')
    expect(nameError('Ab')).toContain('3 to 64')
    expect(nameError('abc')).toContain('upper-case')
    expect(nameError('Health1')).toBe('')
  })
  it('checks texts after trimming', () => {
    expect(textError('Description', '   ')).toBe('Description is required.')
    expect(textError('Description', 'x'.repeat(1001))).toContain('1000')
    expect(textError('Description', ' ok ')).toBe('')
  })
})

describe('browser syntax check (D13, D60)', () => {
  it('passes valid Turtle', () => {
    expect(checkTurtle('<http://a/s> <http://a/p> "o" .', 'https://x/').ok).toBe(true)
  })
  it('reports the line of an error', () => {
    const result = checkTurtle('@prefix ex: <http://e.org/> .\nex:a ex:b .\n', 'https://x/')
    expect(result.ok).toBe(false)
    expect(result.message).toContain('line 2')
  })
})

describe('formatting (D6, D60)', () => {
  it('formats dates in UTC', () => {
    expect(formatDate('2026-11-18T09:27:00Z')).toBe('2026-11-18 09:27 UTC')
    expect(formatDate(undefined)).toBe('')
    expect(formatDate('nonsense')).toBe('nonsense')
  })
  it('labels statuses', () => {
    expect(statusLabel('ACTIVE')).toBe('Active')
    expect(statusLabel('DEPRECATED')).toBe('Deprecated')
  })
})
