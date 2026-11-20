import type { ApiError, Status } from './api';

/** Decided UI texts (D14, D18, D34 to D37, D60, D72, D76, D90, D109). */
export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  noVocabularies: 'No vocabularies found.',
  listFailed: 'The vocabulary list could not be loaded.',
  historyFailed: 'The version history could not be loaded.',
  vocabularyNotFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusFailed: 'The vocabulary status could not be updated.',
  generic: 'Something went wrong. Retry later or contact support.',
  deprecatedVocabulary: 'This vocabulary is deprecated. Its use is discouraged.',
  deprecatedVersion: 'This version is deprecated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
  backToList: 'Back to vocabulary list',
};

export function statusLabel(status: Status): string {
  return status === 'ACTIVE' ? 'Active' : 'Deprecated';
}

/** Dates in UTC as 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const iso = date.toISOString();
  return `${iso.substring(0, 10)} ${iso.substring(11, 16)} UTC`;
}

export function countText(count: number, singular: string, plural: string): string {
  return count === 1 ? `1 ${singular} found` : `${count} ${plural} found`;
}

export function createdText(name: string, version: number): string {
  return `Version ${version} of ${name} was created.`;
}

/** The message of a failed write, without the validation report (shown apart). */
export function writeErrorText(error: ApiError): string {
  if (error.status === 403) return TEXT.notAuthorized;
  if (error.status >= 500 || error.status === 0)
    return `${TEXT.generic} ${error.problem.detail ?? ''}`.trim();
  return error.problem.detail ?? error.message;
}
