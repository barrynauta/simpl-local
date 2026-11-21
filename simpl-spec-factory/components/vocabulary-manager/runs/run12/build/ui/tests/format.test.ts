import { describe, expect, it } from 'vitest';
import {
  errorsFound,
  formatDate,
  isFileTooLarge,
  latestActiveVersion,
  MAX_FILE_BYTES,
  recommendedVersion,
  statusChangeError,
  statusLabel,
  validateName,
  validateText,
  warningsFound,
} from '../src/format';
import { ApiError } from '../src/api';
import { filenameFromDisposition } from '../src/api';
import { submitOutcome } from '../src/submitError';
import { version } from './helpers';

describe('format helpers', () => {
  it('formats dates in UTC (D60)', () => {
    expect(formatDate('2026-11-18T09:27:00Z')).toBe('2026-11-18 09:27 UTC');
    expect(formatDate('2026-01-02T23:59:59+02:00')).toBe('2026-01-02 21:59 UTC');
    expect(formatDate(undefined)).toBe('');
  });

  it('counts errors and warnings (D109)', () => {
    expect(errorsFound(1)).toBe('1 error found');
    expect(errorsFound(3)).toBe('3 errors found');
    expect(warningsFound(1)).toBe('1 warning found');
    expect(warningsFound(2)).toBe('2 warnings found');
  });

  it('checks the 5 MB limit (D15, D76)', () => {
    expect(MAX_FILE_BYTES).toBe(5242880);
    expect(isFileTooLarge(5242880)).toBe(false);
    expect(isFileTooLarge(5242881)).toBe(true);
  });

  it('labels statuses (D6)', () => {
    expect(statusLabel('ACTIVE')).toBe('Active');
    expect(statusLabel('DEPRECATED')).toBe('Deprecated');
  });

  it('validates names and texts (D37)', () => {
    expect(validateName('')).toBe('Name is required.');
    expect(validateName('Ab')).toMatch(/3 to 64/);
    expect(validateName('A'.repeat(65))).toMatch(/3 to 64/);
    expect(validateName('health')).toMatch(/upper-case/);
    expect(validateName('Health-1')).toMatch(/upper-case/);
    expect(validateName('Health1')).toBe('');
    expect(validateText('   ', 'Description')).toBe('Description is required.');
    expect(validateText('x'.repeat(1000), 'Description')).toBe('');
    expect(validateText('x'.repeat(1001), 'Change log')).toMatch(/1000/);
  });

  it('recommends the latest ACTIVE version (D47, D64)', () => {
    const versions = [
      version('A', 3, 'DEPRECATED'),
      version('A', 2, 'ACTIVE'),
      version('A', 1, 'DEPRECATED'),
    ];
    expect(latestActiveVersion(versions)).toBe(2);
    expect(recommendedVersion(versions[2], 'ACTIVE', versions)).toBe(2);
    expect(recommendedVersion(versions[1], 'ACTIVE', versions)).toBeUndefined();
    expect(recommendedVersion(versions[2], 'DEPRECATED', versions)).toBeUndefined();
    expect(latestActiveVersion([version('A', 1, 'DEPRECATED')])).toBeUndefined();
  });

  it('maps status change failures (D72, D114)', () => {
    expect(statusChangeError(new ApiError(404, null)).text).toBe(
      'Vocabulary not found. The status was not changed.',
    );
    const failure = statusChangeError(new ApiError(503, { detail: 'Store down' }));
    expect(failure.text).toBe(
      'The vocabulary status could not be updated. Something went wrong. Retry later or contact support.',
    );
    expect(failure.detail).toBe('Store down');
    expect(statusChangeError(new ApiError(403, null)).text).toBe(
      'You are not authorized to perform this action.',
    );
  });

  it('maps submit failures (D14, D161)', () => {
    expect(submitOutcome(new ApiError(409, { detail: 'Exists' }))).toEqual({
      kind: 'alert',
      text: 'Exists',
      detail: '',
    });
    expect(submitOutcome(new ApiError(400, { issues: [{ detail: 'x' }] })).kind).toBe('report');
    expect(submitOutcome(new ApiError(500, { detail: 'Boom' }))).toEqual({
      kind: 'alert',
      text: 'Something went wrong. Retry later or contact support.',
      detail: 'Boom',
    });
  });

  it('reads the download filename', () => {
    expect(filenameFromDisposition('attachment; filename="A-v2.ttl"', 'x')).toBe('A-v2.ttl');
    expect(filenameFromDisposition(null, 'A.ttl')).toBe('A.ttl');
  });
});
