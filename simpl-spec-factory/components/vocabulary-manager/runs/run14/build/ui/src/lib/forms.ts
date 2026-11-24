import { ApiError, type Issue } from './api';
import { TEXT } from './texts';

export const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

/** Field rules of D37 (free texts, D177). */
export function nameError(value: string): string {
  if (value.length === 0) {
    return 'Name is required.';
  }
  if (value.length < 3 || value.length > 64) {
    return 'Name must have 3 to 64 characters.';
  }
  if (!NAME_PATTERN.test(value)) {
    return 'Name must start with an upper-case letter and contain only letters and digits.';
  }
  return '';
}

export function textError(label: string, value: string, required: boolean): string {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return required ? `${label} is required.` : '';
  }
  if ([...trimmed].length > 1000) {
    return `${label} must have at most 1000 characters.`;
  }
  return '';
}

export interface SubmitFailure {
  report: Issue[];
  message: string;
}

/** Splits a failed submit into the validation report (D14, D90) or one alert text (D161, D60, D18). */
export function submitFailure(error: unknown): SubmitFailure {
  if (error instanceof ApiError) {
    const issues = error.problem.issues ?? [];
    const contentIssues = issues.filter((issue) => issue.name === 'vocabularyFile' && issue.location);
    if (error.status === 400 && contentIssues.length > 0) {
      return { report: contentIssues, message: '' };
    }
    if (error.status === 403) {
      return { report: [], message: TEXT.notAuthorized };
    }
    if (error.status >= 500) {
      return { report: [], message: `${TEXT.generic} ${error.problem.detail ?? ''}`.trim() };
    }
    const details = issues.map((issue) => issue.detail).filter((detail) => !!detail);
    const text = [error.problem.detail, ...details].filter((part) => !!part).join(' ');
    return { report: [], message: text || error.message };
  }
  return { report: [], message: TEXT.generic };
}
