import { reactive } from 'vue';
import type { ValidationWarning } from './api';

export const TEXT = {
  notAuthorized: 'You are not authorized to perform this action.',
  genericError: 'Something went wrong. Retry later or contact support.',
  listLoadError: 'The vocabulary list could not be loaded.',
  historyLoadError: 'The version history could not be loaded.',
  notFound: 'Vocabulary not found.',
  statusNotFound: 'Vocabulary not found. The status was not changed.',
  statusFailed: 'The vocabulary status could not be updated.',
  noVocabularies: 'No vocabularies found.',
  vocabularyDeprecated: 'This vocabulary is deprecated. Its use is discouraged.',
  versionDeprecated: 'This version is deprecated.',
  syntaxPassed: 'Syntax check passed.',
  fileTooLarge: 'The file is larger than 5 MB.',
};

/** A one-shot notice carried to the details page after a creation (D72, D113). */
export interface CreationNotice {
  message: string;
  warnings: ValidationWarning[];
}

export const pendingNotice = reactive<{ value: CreationNotice | null }>({ value: null });

export function takeNotice(): CreationNotice | null {
  const notice = pendingNotice.value;
  pendingNotice.value = null;
  return notice;
}
