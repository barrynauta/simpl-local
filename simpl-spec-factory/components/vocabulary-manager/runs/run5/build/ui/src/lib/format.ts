/** Dates in UTC as 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined | null): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ` +
    `${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())} UTC`
  );
}

/** File name from a Content-Disposition header. */
export function fileNameFrom(disposition: string | null, fallback: string): string {
  if (!disposition) {
    return fallback;
  }
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  return match ? match[1] : fallback;
}
