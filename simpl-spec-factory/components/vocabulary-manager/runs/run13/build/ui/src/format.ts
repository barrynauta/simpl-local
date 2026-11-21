import type { Status } from './api';

/** Formats an ISO 8601 time as 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ` +
    `${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())} UTC`
  );
}

export function statusLabel(status: Status | string): string {
  return status === 'DEPRECATED' ? 'Deprecated' : 'Active';
}

export function countText(n: number, singular: string, plural: string): string {
  return n === 1 ? `1 ${singular} found` : `${n} ${plural} found`;
}
