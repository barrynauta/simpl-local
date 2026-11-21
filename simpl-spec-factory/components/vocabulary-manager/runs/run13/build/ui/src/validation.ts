export const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

export function validateName(value: string): string {
  if (!value) return 'Name is required.';
  if (value.length < 3 || value.length > 64) return 'Name must be 3 to 64 characters long.';
  if (!NAME_PATTERN.test(value))
    return 'Name must start with an upper-case letter and contain only letters and digits.';
  return '';
}

export function validateText(value: string, label: string): string {
  const trimmed = value.trim();
  if (!trimmed) return `${label} is required.`;
  if (trimmed.length > 1000) return `${label} must be at most 1000 characters long.`;
  return '';
}
