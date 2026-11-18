import { describe, expect, it } from 'vitest';
import { checkTurtle, columnOf } from './turtleCheck';
import { latestActiveVersion, nameError, statusLabel, textError, isEffectivelyDeprecated } from './labels';
import type { VocabularyVersion } from './types';

describe('checkTurtle', () => {
  it('accepts valid Turtle', () => {
    expect(checkTurtle('@prefix ex: <http://e/> .\nex:a ex:b "c" .')).toBeUndefined();
  });

  it('reports line and column of a syntax error', () => {
    const error = checkTurtle('@prefix ex: <http://e/> .\nex:a ex:b ;;; .\nex:c ex:d ex:e');
    expect(error).toBeDefined();
    expect(error!.line).toBeGreaterThanOrEqual(2);
    expect(error!.column).toBeGreaterThanOrEqual(1);
  });

  it('finds the token column', () => {
    expect(columnOf('a\n  bad token', 2, 'bad')).toBe(3);
    expect(columnOf('a\n  x', 2, undefined)).toBe(3);
    expect(columnOf('', 5, 'x')).toBe(1);
  });
});

describe('labels and rules', () => {
  const version = (n: number, status: 'ACTIVE' | 'DEPRECATED') =>
    ({ version: n, status }) as VocabularyVersion;

  it('labels statuses', () => {
    expect(statusLabel('ACTIVE')).toBe('Active');
    expect(statusLabel('DEPRECATED')).toBe('Deprecated');
  });

  it('finds the latest ACTIVE version', () => {
    expect(latestActiveVersion([version(1, 'ACTIVE'), version(3, 'DEPRECATED'), version(2, 'ACTIVE')])).toBe(
      2,
    );
    expect(latestActiveVersion([version(1, 'DEPRECATED')])).toBeUndefined();
  });

  it('knows effective deprecation', () => {
    expect(isEffectivelyDeprecated(version(1, 'ACTIVE'), 'DEPRECATED')).toBe(true);
    expect(isEffectivelyDeprecated(version(1, 'ACTIVE'), 'ACTIVE')).toBe(false);
  });

  it('checks names and texts', () => {
    expect(nameError('')).toBeDefined();
    expect(nameError('ab')).toBeDefined();
    expect(nameError('lower')).toBeDefined();
    expect(nameError('Good1')).toBeUndefined();
    expect(textError('Description', '  ')).toBe('Description is required.');
    expect(textError('Description', 'x'.repeat(1001))).toBeDefined();
    expect(textError('Description', 'ok')).toBeUndefined();
  });
});
