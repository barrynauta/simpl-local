import { describe, expect, it } from 'vitest'
import { ACTIONS, statusLabel } from './labels'

describe('labels', () => {
  it('maps every D37 status to its label', () => {
    expect(statusLabel('REACTIVATION_REQUESTED')).toBe('Reactivation requested')
    expect(statusLabel('ACTIVE')).toBe('Active')
  })
  it('marks the actions that need a reason', () => {
    expect(ACTIONS.SUSPEND.reason).toBe('required')
    expect(ACTIONS.DENY_REVOCATION.reason).toBe('required')
    expect(ACTIONS.REACTIVATE.reason).toBe('optional')
  })
})
