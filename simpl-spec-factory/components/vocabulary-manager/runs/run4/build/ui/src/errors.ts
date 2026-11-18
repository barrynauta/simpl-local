import { ApiError } from './api';
import { NOT_AUTHORIZED, RETRY_ADVICE } from './labels';

export interface ShownError {
  message: string;
  detail?: string;
}

/** What the UI says about a failed call: 403 is the authorisation message (D18), 5xx the retry advice (D24, D44). */
export function describeError(error: unknown, fallback: string): ShownError {
  if (error instanceof ApiError) {
    if (error.status === 403) return { message: NOT_AUTHORIZED };
    if (error.status === 0 || error.status >= 500)
      return { message: RETRY_ADVICE, detail: error.problem.detail };
    return { message: fallback, detail: error.problem.detail };
  }
  return { message: fallback, detail: error instanceof Error ? error.message : undefined };
}

export function statusOf(error: unknown): number | undefined {
  return error instanceof ApiError ? error.status : undefined;
}
