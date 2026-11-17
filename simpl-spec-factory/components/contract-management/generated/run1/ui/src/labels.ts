// D37 UI labels.
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
  TERMINATED: 'Terminated',
}

export function statusLabel(s: string | null | undefined): string {
  return s ? STATUS_LABELS[s] ?? s : ''
}

// Lifecycle actions: test id, button label and whether a reason is required (D37, D39).
export const ACTIONS: Record<string, { testId: string; label: string; reasonRequired: boolean }> = {
  ACCEPT: { testId: 'action-accept', label: 'Accept', reasonRequired: false },
  REJECT: { testId: 'action-reject', label: 'Reject', reasonRequired: false },
  SUSPEND: { testId: 'action-suspend', label: 'Suspend', reasonRequired: true },
  REQUEST_REACTIVATION: { testId: 'action-request-reactivation', label: 'Request reactivation', reasonRequired: true },
  REACTIVATE: { testId: 'action-reactivate', label: 'Reactivate', reasonRequired: false },
  REQUEST_REVOCATION: { testId: 'action-request-revocation', label: 'Request revocation', reasonRequired: true },
  ACCEPT_REVOCATION: { testId: 'action-accept-revocation', label: 'Accept revocation', reasonRequired: false },
  DENY_REVOCATION: { testId: 'action-deny-revocation', label: 'Deny revocation', reasonRequired: true },
  TERMINATE: { testId: 'action-terminate', label: 'Terminate', reasonRequired: true },
}

export function counterpartyName(item: { partyRole?: string; providerName?: string | null; consumerName?: string | null }): string {
  return (item.partyRole === 'CONSUMER' ? item.providerName : item.consumerName) ?? ''
}
