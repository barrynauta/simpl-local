/** Decided UI texts (D14, D18, D34 to D37, D46, D60, D72, D76, D109, D115). */
export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  noVocabularies: 'No vocabularies found.',
  listLoadFailed: 'The vocabulary list could not be loaded.',
  historyLoadFailed: 'The version history could not be loaded.',
  vocabularyNotFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusFailed: 'The vocabulary status could not be updated.',
  genericFailure: 'Something went wrong. Retry later or contact support.',
  vocabularyDeprecated: 'This vocabulary is deprecated. Its use is discouraged.',
  versionDeprecated: 'This version is deprecated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
  backToList: 'Back to vocabulary list',
  initialChangelog: 'Initial version.',
  downloadFailed: 'The vocabulary content could not be downloaded.',
} as const;
