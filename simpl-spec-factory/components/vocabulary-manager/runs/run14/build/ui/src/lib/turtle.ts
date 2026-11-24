import { Parser } from 'n3';

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

export interface SyntaxResult {
  ok: boolean;
  line?: number;
  column?: number;
  message?: string;
}

/**
 * The browser syntax check of D13 and D60: parses the file as Turtle with N3.js. The server check stays
 * authoritative (D200).
 */
export function checkTurtle(text: string): SyntaxResult {
  try {
    new Parser({ format: 'text/turtle', baseIRI: 'https://example.invalid/base' }).parse(text);
    return { ok: true };
  } catch (error) {
    const err = error as Error & { context?: { line?: number } };
    const message = err.message ?? 'Invalid Turtle syntax';
    const lineMatch = /line (\d+)/i.exec(message);
    const line = err.context?.line ?? (lineMatch ? Number(lineMatch[1]) : undefined);
    return { ok: false, line, message: message.replace(/\s*on line \d+\.?/i, '') };
  }
}

export function syntaxErrorText(result: SyntaxResult): string {
  const where = result.line !== undefined ? `line ${result.line}` : 'unknown line';
  const column = result.column !== undefined ? `, column ${result.column}` : '';
  return `Syntax error at ${where}${column}: ${result.message ?? ''}`.trim();
}
