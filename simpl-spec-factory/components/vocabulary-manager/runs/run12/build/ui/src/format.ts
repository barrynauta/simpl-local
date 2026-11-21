import type { VocabularyStatus, VocabularyVersion } from './api';
import { ApiError } from './api';
import { TEXT } from './texts';

/** D15, D76: the upload limit of the vocabulary file part. */
export const MAX_FILE_BYTES = 5 * 1024 * 1024;

export function isFileTooLarge(size: number): boolean {
  return size > MAX_FILE_BYTES;
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

/** D60: dates in UTC as 'YYYY-MM-DD HH:mm UTC'. */
export function formatDate(value: string | undefined | null): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return (
    `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())} ` +
    `${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())} UTC`
  );
}

/** D6: 'Active' for ACTIVE, 'Deprecated' for DEPRECATED. */
export function statusLabel(status: VocabularyStatus | string): string {
  if (status === 'ACTIVE') return 'Active';
  if (status === 'DEPRECATED') return 'Deprecated';
  return status;
}

/** D109 */
export function errorsFound(n: number): string {
  return n === 1 ? '1 error found' : `${n} errors found`;
}

/** D109 */
export function warningsFound(n: number): string {
  return n === 1 ? '1 warning found' : `${n} warnings found`;
}

export const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;

/** D37: name rules, checked in the form before submit. */
export function validateName(value: string): string {
  if (value.length === 0) return 'Name is required.';
  if (value.length < 3 || value.length > 64) return 'Name must be 3 to 64 characters.';
  if (!NAME_PATTERN.test(value)) {
    return 'Name must start with an upper-case letter and contain only letters and digits.';
  }
  return '';
}

/** D37: 1 to 1000 characters after trimming. */
export function validateText(value: string, label: string): string {
  const trimmed = value.trim();
  if (trimmed.length === 0) return `${label} is required.`;
  if (trimmed.length > 1000) return `${label} must be at most 1000 characters.`;
  return '';
}

/** The problem detail of an API error, or its message. */
export function problemDetail(error: unknown): string {
  if (error instanceof ApiError) return error.problem?.detail ?? error.message;
  if (error instanceof Error) return error.message;
  return String(error);
}

export function isServerFailure(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 0 || error.status >= 500);
}

export function isStatus(error: unknown, status: number): boolean {
  return error instanceof ApiError && error.status === status;
}

/** D72, D114: the one alert text of a failed status change. */
export function statusChangeError(error: unknown): { text: string; detail: string } {
  if (isStatus(error, 403)) return { text: TEXT.notAuthorized, detail: '' };
  if (isStatus(error, 404)) return { text: TEXT.statusNotFound, detail: '' };
  if (isServerFailure(error)) {
    return { text: `${TEXT.statusFailed} ${TEXT.genericFailure}`, detail: problemDetail(error) };
  }
  return { text: TEXT.statusFailed, detail: problemDetail(error) };
}

/** D3: a version is effectively deprecated when it or its vocabulary is DEPRECATED. */
export function isEffectivelyDeprecated(
  version: VocabularyVersion,
  vocabularyStatus: VocabularyStatus | undefined,
): boolean {
  return version.status === 'DEPRECATED' || vocabularyStatus === 'DEPRECATED';
}

/** D5: the highest-numbered version whose version status is ACTIVE. */
export function latestActiveVersion(versions: VocabularyVersion[]): number | undefined {
  let best: number | undefined;
  for (const v of versions) {
    if (v.status === 'ACTIVE' && (best === undefined || v.version > best)) best = v.version;
  }
  return best;
}

/**
 * D36, D47, D64: the version to recommend for an effectively deprecated version, or undefined
 * when the vocabulary is DEPRECATED, when no ACTIVE version exists or when it is the version shown.
 */
export function recommendedVersion(
  version: VocabularyVersion,
  vocabularyStatus: VocabularyStatus | undefined,
  versions: VocabularyVersion[],
): number | undefined {
  if (vocabularyStatus === 'DEPRECATED') return undefined;
  const latest = latestActiveVersion(versions);
  if (latest === undefined || latest === version.version) return undefined;
  return latest;
}
