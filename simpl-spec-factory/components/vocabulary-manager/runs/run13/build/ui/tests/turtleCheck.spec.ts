import { describe, expect, it } from 'vitest';
import { checkFile, checkTurtle, errorText, MAX_FILE_BYTES } from '../src/turtleCheck';

describe('checkTurtle', () => {
  it('passes valid Turtle', () => {
    const result = checkTurtle('@prefix ex: <http://example.org/> .\nex:a ex:b "c" .\n');
    expect(result.ok).toBe(true);
    expect(errorText(result)).toBe('Syntax check passed.');
  });

  it('reports the line of a syntax error', () => {
    const result = checkTurtle('@prefix ex: <http://example.org/> .\n\nex:a ex:b ;; .\n');
    expect(result.ok).toBe(false);
    expect(result.line).toBe(3);
    expect(errorText(result)).toMatch(/^Line 3: Turtle syntax error/);
  });

  it('formats a column when one is known', () => {
    expect(errorText({ ok: false, line: 2, column: 5, message: 'x' })).toBe('Line 2, column 5: x');
    expect(errorText({ ok: false, message: 'x' })).toBe('x');
  });

  it('refuses files over 5 MB', async () => {
    const big = new File([new Uint8Array(MAX_FILE_BYTES + 1)], 'big.ttl');
    const result = await checkFile(big);
    expect(result.ok).toBe(false);
    expect(result.message).toBe('The file is larger than 5 MB.');
  });

  it('checks the text of a small file', async () => {
    const file = new File(['<http://a> <http://b> <http://c> .'], 'a.ttl');
    expect((await checkFile(file)).ok).toBe(true);
  });
});
