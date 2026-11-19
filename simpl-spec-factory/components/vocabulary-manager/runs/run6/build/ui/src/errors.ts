import { ApiError } from "./api";
import { TEXTS } from "./texts";

/** Describes an API failure for the user: 403, 5xx and others (decisions D18, D24, D44, D60). */
export function describeError(error: unknown): { message: string; detail: string } {
  if (error instanceof ApiError) {
    const detail = error.problem.detail ?? "";
    if (error.status === 403) {
      return { message: TEXTS.notAuthorized, detail: "" };
    }
    if (error.status >= 500) {
      return { message: TEXTS.genericError, detail };
    }
    return { message: detail || error.problem.title || `HTTP ${error.status}`, detail: "" };
  }
  return { message: TEXTS.genericError, detail: error instanceof Error ? error.message : "" };
}
