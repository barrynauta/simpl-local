/** Formats an ISO 8601 UTC time as 'YYYY-MM-DD HH:mm UTC' (decision D60). */
export function formatUtc(value: string | undefined | null): string {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ` +
    `${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())} UTC`
  );
}

/** Field rules of decision D37, shared by the forms. */
export const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

export function nameError(name: string): string | null {
  if (!name) {
    return "Name is required.";
  }
  if (name.length < 3 || name.length > 64 || !NAME_PATTERN.test(name)) {
    return "Name must be 3 to 64 characters, start with an upper-case letter and contain only letters and digits.";
  }
  return null;
}

export function textError(label: string, value: string): string | null {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return `${label} is required.`;
  }
  if ([...trimmed].length > 1000) {
    return `${label} must be at most 1000 characters.`;
  }
  return null;
}
