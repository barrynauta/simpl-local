/** Decided UI texts (D18, D34 to D36, D46, D60, D72, D76). One place, so tests and views use the same words. */
export const TEXT = {
  menu: 'Vocabularies',
  notAuthorized: 'You are not authorized to perform this action.',
  noVocabularies: 'No vocabularies found.',
  listError: 'The vocabulary list could not be loaded.',
  historyError: 'The version history could not be loaded.',
  notFound: 'Vocabulary not found.',
  backToList: 'Back to vocabulary list',
  deprecatedVocabulary: 'This vocabulary is deprecated. Its use is discouraged.',
  deprecatedVersion: 'This version is deprecated.',
  genericError: 'Something went wrong. Retry later or contact support.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusError: 'The vocabulary status could not be updated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
  newVocabulary: 'New Vocabulary +',
};

/** UI label of a status value (D6). */
export function statusLabel(status: string): string {
  return status === 'DEPRECATED' ? 'Deprecated' : 'Active';
}

/** Dates in UTC as 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined): string {
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
