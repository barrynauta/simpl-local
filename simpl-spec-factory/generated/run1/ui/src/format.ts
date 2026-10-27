export function formatDate(value?: string): string {
  if (!value) return ''
  const d = new Date(value)
  return isNaN(d.getTime()) ? value : d.toLocaleString()
}

export function statusLabel(status: string): string {
  return status === 'ACTIVE' ? 'Active' : status === 'DEPRECATED' ? 'Deprecated' : status
}
