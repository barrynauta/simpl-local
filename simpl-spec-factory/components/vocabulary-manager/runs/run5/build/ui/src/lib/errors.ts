import { ApiError } from './api';
import { GENERIC_ERROR, NOT_AUTHORIZED } from './messages';

/** Lines to show for a failed API call (D18, D24, D44, D60). */
export function errorLines(error: unknown, context?: string): string[] {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return [NOT_AUTHORIZED];
    }
    const detail = error.problem.detail ?? '';
    const lines: string[] = [];
    if (context) {
      lines.push(context);
    }
    if (error.status >= 500 || error.status === 0) {
      lines.push(GENERIC_ERROR);
    }
    if (detail) {
      lines.push(detail);
    }
    return lines;
  }
  return [context ?? GENERIC_ERROR, GENERIC_ERROR];
}
