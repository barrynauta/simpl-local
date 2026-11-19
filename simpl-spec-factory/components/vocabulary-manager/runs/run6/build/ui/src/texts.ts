// UI texts fixed by the decisions (D13, D14, D18, D34, D35, D36, D46, D60). Tests use these texts.
export const TEXTS = {
  menu: "Vocabularies",
  newVocabulary: "New Vocabulary +",
  update: "Update",
  deprecate: "Deprecate",
  activate: "Activate",
  viewDetails: "View vocabulary details",
  download: "Download vocabulary",
  downloadVersion: "Download",
  submit: "Submit",
  cancel: "Cancel",
  backToList: "Back to vocabulary list",
  noVocabularies: "No vocabularies found.",
  listLoadError: "The vocabulary list could not be loaded.",
  historyLoadError: "The version history could not be loaded.",
  vocabularyNotFound: "Vocabulary not found.",
  notAuthorized: "You are not authorized to perform this action.",
  genericError: "Something went wrong. Retry later or contact support.",
  statusUpdateError: "The vocabulary status could not be updated.",
  syntaxPassed: "Syntax check passed.",
  vocabularyDeprecated: "This vocabulary is deprecated. Its use is discouraged.",
  versionDeprecated: "This version is deprecated.",
  latestActive: (n: number) => `Latest ACTIVE version: ${n}`,
  versionCreated: (n: number, name: string) => `Version ${n} of ${name} was created.`,
  statusChanged: (name: string, label: string) => `Status of ${name} changed to ${label}.`,
  submitCreates: (next: number) =>
    `Submitting creates version ${next} and deprecates every older ACTIVE version.`,
  currentContent: (name: string) => `${name}.ttl (current content)`,
  tooLarge:
    "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please upload a smaller Turtle file.",
  initialChangelog: "Initial version.",
};

export const STATUS_LABEL: Record<string, string> = { ACTIVE: "Active", DEPRECATED: "Deprecated" };

export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
