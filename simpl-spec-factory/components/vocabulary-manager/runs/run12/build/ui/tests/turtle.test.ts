import { describe, expect, it } from 'vitest';
import { checkTurtle, describeSyntaxError } from '../src/turtle';

describe('browser Turtle check (D13)', () => {
  it('accepts valid Turtle', () => {
    const ttl = '@prefix ex: <http://example.org/> .\nex:a ex:b "c" .\n';
    expect(checkTurtle(ttl)).toEqual({ ok: true });
  });

  it('reports the line of the first syntax error', () => {
    const ttl = '@prefix ex: <http://example.org/> .\nex:a ex:b "c" .\nex:d ex:e ;; .\n';
    const result = checkTurtle(ttl);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.line).toBe(3);
    expect(describeSyntaxError(result)).toMatch(/^Syntax error at line 3/);
  });
});
