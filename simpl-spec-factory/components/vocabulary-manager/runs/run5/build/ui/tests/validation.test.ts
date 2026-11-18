import { describe, expect, it } from 'vitest';
import { checkTurtle, nameError, textError } from '../src/lib/validation';
import { formatDate, fileNameFrom } from '../src/lib/format';
import { nextVersionLine, statusChanged, statusLabel, versionCreated } from '../src/lib/messages';

describe('form rules (D37)', () => {
  it('accepts a valid name', () => expect(nameError('HealthVocabulary1')).toBeNull());
  it('requires a name', () => expect(nameError('')).toBe('Name is required.'));
  it('rejects a lower-case start', () => expect(nameError('health')).not.toBeNull());
  it('rejects a short name', () => expect(nameError('Ab')).not.toBeNull());
  it('rejects a long name', () => expect(nameError('A' + 'b'.repeat(64))).not.toBeNull());
  it('requires a description', () =>
    expect(textError('Description', '   ')).toBe('Description is required.'));
  it('limits text to 1000 characters', () =>
    expect(textError('Change log', 'x'.repeat(1001))).not.toBeNull());
  it('accepts 1000 characters', () => expect(textError('Change log', 'x'.repeat(1000))).toBeNull());
});

describe('upload check (D13, D15)', () => {
  it('passes valid Turtle', () => {
    const result = checkTurtle('@prefix ex: <http://example.org/> .\nex:a ex:b ex:c .', 50);
    expect(result).toEqual({ ok: true, message: 'Syntax check passed.' });
  });
  it('reports the line of a syntax error', () => {
    const result = checkTurtle('@prefix ex: <http://example.org/> .\nex:a ex:b .\n', 50);
    expect(result.ok).toBe(false);
    expect(result.line).toBe(2);
    expect(result.message).toContain('line 2');
  });
  it('refuses files over 5 MB', () => {
    const result = checkTurtle('', 5 * 1024 * 1024 + 1);
    expect(result.ok).toBe(false);
    expect(result.message).toContain('5 MB');
  });
});

describe('texts (D14, D46, D60)', () => {
  it('formats dates in UTC', () => expect(formatDate('2026-11-18T09:27:00Z')).toBe('2026-11-18 09:27 UTC'));
  it('keeps empty dates empty', () => expect(formatDate(undefined)).toBe(''));
  it('reads a file name', () =>
    expect(fileNameFrom('attachment; filename="A-v2.ttl"', 'x')).toBe('A-v2.ttl'));
  it('falls back without a header', () => expect(fileNameFrom(null, 'x.ttl')).toBe('x.ttl'));
  it('confirms a creation', () =>
    expect(versionCreated(2, 'Health')).toBe('Version 2 of Health was created.'));
  it('confirms a status change', () =>
    expect(statusChanged('Health', 'DEPRECATED')).toBe('Status of Health changed to Deprecated.'));
  it('labels statuses', () => expect(statusLabel('ACTIVE')).toBe('Active'));
  it('announces the next version', () =>
    expect(nextVersionLine(3)).toBe(
      'Submitting creates version 4 and deprecates every older ACTIVE version.',
    ));
});
