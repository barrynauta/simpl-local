// D37 status labels and the D72 test hooks for actions.
export const STATUS_LABELS: Record<string, string> = {
  REQUESTED: 'Requested',
  ACCEPTED: 'Accepted',
  REJECTED: 'Rejected',
  SIGNED: 'Signed',
  ACTIVE: 'Active',
  FAILED: 'Failed',
  SUSPENDED: 'Suspended',
  REACTIVATION_REQUESTED: 'Reactivation requested',
  REVOCATION_REQUESTED: 'Revocation requested',
  REVOKED: 'Revoked',
  TERMINATED: 'Terminated'
}

export function statusLabel(status: string | null | undefined): string {
  if (!status) return ''
  return STATUS_LABELS[status] ?? status
}

export interface ActionInfo {
  testId: string
  label: string
  reason: 'required' | 'optional' | 'none'
}

export const ACTIONS: Record<string, ActionInfo> = {
  ACCEPT: { testId: 'action-accept', label: 'Accept', reason: 'none' },
  REJECT: { testId: 'action-reject', label: 'Reject', reason: 'none' },
  SUSPEND: { testId: 'action-suspend', label: 'Suspend', reason: 'required' },
  REQUEST_REACTIVATION: { testId: 'action-request-reactivation', label: 'Request reactivation', reason: 'required' },
  REACTIVATE: { testId: 'action-reactivate', label: 'Reactivate', reason: 'optional' },
  REQUEST_REVOCATION: { testId: 'action-request-revocation', label: 'Request revocation', reason: 'required' },
  ACCEPT_REVOCATION: { testId: 'action-accept-revocation', label: 'Accept revocation', reason: 'optional' },
  DENY_REVOCATION: { testId: 'action-deny-revocation', label: 'Deny revocation', reason: 'required' },
  TERMINATE: { testId: 'action-terminate', label: 'Terminate', reason: 'required' },
  UPDATE_CONSENT: { testId: 'action-update-consent', label: 'Update consent', reason: 'none' }
}
