/** Field rules of D37, the same limits as the API. */
const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

export function nameError(value: string): string | null {
  if (value.length === 0) {
    return 'Name is required.';
  }
  if (value.length < 3 || value.length > 64) {
    return 'Name must have 3 to 64 characters.';
  }
  if (!NAME_PATTERN.test(value)) {
    return 'Name must start with an upper-case letter and contain only letters and digits.';
  }
  return null;
}

export function textError(label: string, value: string): string | null {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return `${label} is required.`;
  }
  if ([...trimmed].length > 1000) {
    return `${label} may have at most 1000 characters.`;
  }
  return null;
}
