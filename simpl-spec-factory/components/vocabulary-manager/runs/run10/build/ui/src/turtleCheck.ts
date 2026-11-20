import { Parser } from 'n3';

/** The result of the browser-side syntax check of D13. */
export interface SyntaxResult {
  ok: boolean;
  line?: number;
  column?: number;
  message?: string;
}

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

/** Parses Turtle with the N3.js parser library; returns the first syntax error with its line (and column if given). */
export function checkTurtle(text: string): SyntaxResult {
  try {
    new Parser({ format: 'text/turtle', baseIRI: 'https://upload.invalid/' }).parse(text);
    return { ok: true };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    const context = (error as { context?: { line?: number } }).context;
    const lineMatch = /line (\d+)/i.exec(message);
    const line = context?.line ?? (lineMatch ? Number(lineMatch[1]) : undefined);
    const columnMatch = /column (\d+)/i.exec(message);
    return { ok: false, line, column: columnMatch ? Number(columnMatch[1]) : undefined, message };
  }
}

/** The text shown under the file field for a syntax error (D60: column only when the parser gives one). */
export function syntaxErrorText(result: SyntaxResult): string {
  const where =
    result.line === undefined
      ? ''
      : result.column === undefined
        ? ` (line ${result.line})`
        : ` (line ${result.line}, column ${result.column})`;
  return `Syntax error${where}: ${result.message ?? ''}`.trim();
}
