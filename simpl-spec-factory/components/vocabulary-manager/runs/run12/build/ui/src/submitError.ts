import { ApiError, type Problem } from './api';
import { isServerFailure, isStatus, problemDetail } from './format';
import { TEXT } from './texts';

export type SubmitOutcome =
  { kind: 'report'; problem: Problem } | { kind: 'alert'; text: string; detail: string };

/**
 * D14, D37, D60, D161: a 400 with issues is the validation report; 409 and 413 and other
 * client errors show the problem detail as the API sends it; 500 and 503 show the D60 text.
 */
export function submitOutcome(error: unknown): SubmitOutcome {
  if (isStatus(error, 403)) return { kind: 'alert', text: TEXT.notAuthorized, detail: '' };
  if (isServerFailure(error)) {
    return { kind: 'alert', text: TEXT.genericFailure, detail: problemDetail(error) };
  }
  if (error instanceof ApiError && error.status === 400 && error.problem?.issues?.length) {
    return { kind: 'report', problem: error.problem };
  }
  return { kind: 'alert', text: problemDetail(error), detail: '' };
}
