/** Fixed UI texts of the decisions (D14, D18, D34 to D36, D46, D60). */
export const NOT_AUTHORIZED = 'You are not authorized to perform this action.';
export const GENERIC_ERROR = 'Something went wrong. Retry later or contact support.';
export const LIST_NOT_LOADED = 'The vocabulary list could not be loaded.';
export const NO_VOCABULARIES = 'No vocabularies found.';
export const VOCABULARY_NOT_FOUND = 'Vocabulary not found.';
export const HISTORY_NOT_LOADED = 'The version history could not be loaded.';
export const VOCABULARY_DEPRECATED = 'This vocabulary is deprecated. Its use is discouraged.';
export const VERSION_DEPRECATED = 'This version is deprecated.';
export const SYNTAX_PASSED = 'Syntax check passed.';
export const STATUS_NOT_UPDATED = 'The vocabulary status could not be updated.';
export const FILE_TOO_LARGE =
  'Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please upload a smaller Turtle file.';
export const UPLOAD_LIMIT_BYTES = 5 * 1024 * 1024;

export function versionCreated(version: number, name: string): string {
  return `Version ${version} of ${name} was created.`;
}

export function statusChanged(name: string, status: string): string {
  return `Status of ${name} changed to ${statusLabel(status)}.`;
}

export function nextVersionLine(latest: number): string {
  return `Submitting creates version ${latest + 1} and deprecates every older ACTIVE version.`;
}

/** UI labels of the status values (D6). */
export function statusLabel(status: string): string {
  return status === 'DEPRECATED' ? 'Deprecated' : status === 'ACTIVE' ? 'Active' : status;
}
