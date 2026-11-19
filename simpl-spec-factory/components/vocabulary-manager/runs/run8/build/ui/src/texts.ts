/** Decided UI texts (D14, D18, D34, D35, D36, D46, D60, D72, D76). */
export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  genericError: 'Something went wrong. Retry later or contact support.',
  listLoadError: 'The vocabulary list could not be loaded.',
  noVocabularies: 'No vocabularies found.',
  historyLoadError: 'The version history could not be loaded.',
  vocabularyNotFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusFailed: 'The vocabulary status could not be updated.',
  vocabularyDeprecated: 'This vocabulary is deprecated. Its use is discouraged.',
  versionDeprecated: 'This version is deprecated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
  backToList: 'Back to vocabulary list',
  initialChangelog: 'Initial version.',
};

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

export function versionCreated(version: number, name: string): string {
  return `Version ${version} of ${name} was created.`;
}

export function statusChanged(name: string, status: string): string {
  return `Status of ${name} changed to ${status === 'DEPRECATED' ? 'Deprecated' : 'Active'}.`;
}

export function nextVersionLine(latest: number): string {
  return `Submitting creates version ${latest + 1} and deprecates every older ACTIVE version.`;
}
