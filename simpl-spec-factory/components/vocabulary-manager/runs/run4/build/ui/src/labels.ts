import type { VocabularyStatus, VocabularyVersion } from './types';

/** UI labels of the stored status values (decision D6). */
export function statusLabel(status: VocabularyStatus): string {
  return status === 'ACTIVE' ? 'Active' : 'Deprecated';
}

export function otherStatus(status: VocabularyStatus): VocabularyStatus {
  return status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
}

export const NOT_AUTHORIZED = 'You are not authorized to perform this action.';
export const VOCABULARY_DEPRECATED = 'This vocabulary is deprecated. Its use is discouraged.';
export const VERSION_DEPRECATED = 'This version is deprecated.';
export const RETRY_ADVICE = 'The operation failed. Please retry or contact support.';
export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
export const UPLOAD_TOO_LARGE =
  'Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please upload a smaller Turtle file.';

/** A version is effectively deprecated when its own or its vocabulary's status is DEPRECATED (decision D3). */
export function isEffectivelyDeprecated(
  version: VocabularyVersion,
  vocabularyStatus: VocabularyStatus,
): boolean {
  return version.status === 'DEPRECATED' || vocabularyStatus === 'DEPRECATED';
}

/** The highest-numbered version whose own status is ACTIVE (decision D5). */
export function latestActiveVersion(versions: VocabularyVersion[]): number | undefined {
  const active = versions.filter((v) => v.status === 'ACTIVE').map((v) => v.version);
  return active.length === 0 ? undefined : Math.max(...active);
}

/** Field rules of decision D37; returns an error text or undefined. */
export function nameError(name: string): string | undefined {
  if (!name) return 'Name is required.';
  if (name.length < 3 || name.length > 64) return 'Name must have 3 to 64 characters.';
  if (!/^[A-Z][a-zA-Z0-9]*$/.test(name))
    return 'Name must start with a capital letter and contain only letters and digits.';
  return undefined;
}

export function textError(label: string, value: string): string | undefined {
  const trimmed = value.trim();
  if (trimmed.length === 0) return `${label} is required.`;
  if (trimmed.length > 1000) return `${label} must have at most 1000 characters.`;
  return undefined;
}
