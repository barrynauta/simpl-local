import { Parser } from 'n3';

export interface SyntaxError {
  message: string;
  line: number;
  column: number;
}

/**
 * Syntax check in the browser (decision D13). The server check on submit is authoritative; this one only gives early
 * feedback. Returns the first syntax error, or undefined when the text parses as Turtle.
 */
export function checkTurtle(text: string): SyntaxError | undefined {
  try {
    new Parser({ format: 'text/turtle', baseIRI: 'urn:x-upload:' }).parse(text);
    return undefined;
  } catch (e) {
    const error = e as Error & { context?: { line?: number; token?: { value?: string } } };
    const line = error.context?.line ?? lineFromMessage(error.message) ?? 1;
    return { message: error.message, line, column: columnOf(text, line, error.context?.token?.value) };
  }
}

function lineFromMessage(message: string): number | undefined {
  const match = /line (\d+)/.exec(message);
  return match ? Number(match[1]) : undefined;
}

/** N3 reports no column; take the position of the offending token in its line, else column 1. */
export function columnOf(text: string, line: number, token: string | undefined): number {
  const lines = text.split(/\r?\n/);
  const content = lines[line - 1] ?? '';
  if (token) {
    const index = content.indexOf(token);
    if (index >= 0) return index + 1;
  }
  const firstNonSpace = content.search(/\S/);
  return firstNonSpace >= 0 ? firstNonSpace + 1 : 1;
}
