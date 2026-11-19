import { ApiError } from './api'

/** Decided UI texts (D18, D34, D35, D36, D60, D72). */
export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  genericError: 'Something went wrong. Retry later or contact support.',
  listError: 'The vocabulary list could not be loaded.',
  historyError: 'The version history could not be loaded.',
  noVocabularies: 'No vocabularies found.',
  vocabularyNotFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusError: 'The vocabulary status could not be updated.',
  vocabularyDeprecated: 'This vocabulary is deprecated. Its use is discouraged.',
  versionDeprecated: 'This version is deprecated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
}

export function problemDetail(error: unknown): string {
  if (error instanceof ApiError) {
    return error.problem.detail ?? ''
  }
  return error instanceof Error ? error.message : ''
}

export function status(error: unknown): number {
  return error instanceof ApiError ? error.status : 0
}
