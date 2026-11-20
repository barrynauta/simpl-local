import { Parser } from 'n3';

/** Result of the browser syntax check (D13, D60). */
export interface SyntaxResult {
  ok: boolean;
  message: string;
}

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

/** Parses Turtle with the N3.js library; reports the first error with its line, and the column when given. */
export function checkTurtle(text: string): SyntaxResult {
  try {
    new Parser({ format: 'text/turtle', baseIRI: 'https://authority.example/base' }).parse(text);
    return { ok: true, message: 'Syntax check passed.' };
  } catch (error) {
    const err = error as Error & { context?: { line?: number; column?: number } };
    const line = err.context?.line ?? /line (\d+)/.exec(err.message)?.[1];
    const column = err.context?.column;
    const where = line ? `line ${line}${column ? `, column ${column}` : ''}` : 'line 1';
    return { ok: false, message: `Syntax error on ${where}: ${err.message}` };
  }
}
