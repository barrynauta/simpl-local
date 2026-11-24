import { ApiError } from './api';

/** Decided UI texts (D18, D34 to D36, D60, D72, D109). */
export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  generic: 'Something went wrong. Retry later or contact support.',
  listLoadError: 'The vocabulary list could not be loaded.',
  historyLoadError: 'The version history could not be loaded.',
  noVocabularies: 'No vocabularies found.',
  vocabularyNotFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusFailed: 'The vocabulary status could not be updated.',
  vocabularyDeprecated: 'This vocabulary is deprecated. Its use is discouraged.',
  versionDeprecated: 'This version is deprecated.',
  fileTooLarge: 'The file is larger than 5 MB.',
  syntaxPassed: 'Syntax check passed.',
  backToList: 'Back to vocabulary list',
};

export function statusLabel(status: string): string {
  return status === 'ACTIVE' ? 'Active' : 'Deprecated';
}

/** 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const iso = date.toISOString();
  return `${iso.slice(0, 10)} ${iso.slice(11, 16)} UTC`;
}

export function countText(count: number, singular: string, plural: string): string {
  return count === 1 ? `1 ${singular}` : `${count} ${plural}`;
}

/** The text of an error message for a failed write (D18, D60, D161). */
export function writeErrorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return TEXT.notAuthorized;
    }
    if (error.status >= 500) {
      return `${TEXT.generic} ${error.problem.detail ?? ''}`.trim();
    }
    return error.problem.detail ?? error.message;
  }
  return TEXT.generic;
}

export function problemDetail(error: unknown): string {
  return error instanceof ApiError ? (error.problem.detail ?? '') : '';
}
