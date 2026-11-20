/** Decided UI texts (D18, D34 to D37, D46, D60, D72, D109, D114). */
export const TEXT = {
  notAuthorized: "You are not authorized to perform this action.",
  listLoadFailed: "The vocabulary list could not be loaded.",
  noVocabularies: "No vocabularies found.",
  vocabularyDeprecated: "This vocabulary is deprecated. Its use is discouraged.",
  versionDeprecated: "This version is deprecated.",
  historyLoadFailed: "The version history could not be loaded.",
  vocabularyNotFound: "Vocabulary not found.",
  statusNotFound: "Vocabulary not found. The status was not changed.",
  statusFailed: "The vocabulary status could not be updated.",
  genericError: "Something went wrong. Retry later or contact support.",
  syntaxPassed: "Syntax check passed.",
  fileTooLarge: "The file is larger than 5 MB.",
  backToList: "Back to vocabulary list",
};

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

export function count(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? "" : "s"} found`;
}

export function statusLabel(status: string): string {
  return status === "DEPRECATED" ? "Deprecated" : "Active";
}

/** 'YYYY-MM-DD HH:mm UTC' (D60). */
export function formatDate(value: string | undefined): string {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const iso = date.toISOString();
  return `${iso.slice(0, 10)} ${iso.slice(11, 16)} UTC`;
}
