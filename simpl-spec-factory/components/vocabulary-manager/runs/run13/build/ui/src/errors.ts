import { ApiError } from './api';
import { TEXT } from './messages';

export function problemDetail(error: unknown): string {
  if (error instanceof ApiError) return error.problem.detail ?? error.problem.title ?? '';
  return error instanceof Error ? error.message : '';
}

function isServerError(error: unknown): boolean {
  return !(error instanceof ApiError) || error.status >= 500;
}

/** Text for a failed form submission: 403, 5xx or the API detail (D18, D60, D161). */
export function submitErrorText(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) return TEXT.notAuthorized;
  if (isServerError(error)) return `${TEXT.genericError} ${problemDetail(error)}`.trim();
  return problemDetail(error);
}

/** Text for a failed status change (D72, D114). */
export function statusErrorText(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) return TEXT.notAuthorized;
  if (error instanceof ApiError && error.status === 404) return TEXT.statusNotFound;
  if (isServerError(error))
    return `${TEXT.statusFailed} ${TEXT.genericError} ${problemDetail(error)}`.trim();
  return problemDetail(error);
}

/** Text for a failed load of the list or the history (D34, D35, D72). */
export function loadErrorText(error: unknown, decided: string): string {
  if (error instanceof ApiError && error.status === 403) return TEXT.notAuthorized;
  return `${decided} ${problemDetail(error)}`.trim();
}
