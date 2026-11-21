import { Parser } from 'n3';

export const MAX_FILE_BYTES = 5 * 1024 * 1024;

export interface TurtleCheckResult {
  ok: boolean;
  line?: number;
  column?: number;
  message: string;
}

/**
 * Syntax check of Turtle content in the browser (D13). The server check on submit stays
 * authoritative; this only gives early feedback.
 */
export function checkTurtle(text: string): TurtleCheckResult {
  try {
    new Parser({ format: 'text/turtle' }).parse(text);
    return { ok: true, message: 'Syntax check passed.' };
  } catch (error) {
    const err = error as Error & { context?: { line?: number } };
    const line = err.context?.line ?? lineFromMessage(err.message);
    return { ok: false, line, message: `Turtle syntax error: ${err.message}` };
  }
}

function lineFromMessage(message: string): number | undefined {
  const match = /line (\d+)/i.exec(message);
  return match ? Number(match[1]) : undefined;
}

export function errorText(result: TurtleCheckResult): string {
  if (result.ok) return result.message;
  const position =
    result.line === undefined
      ? ''
      : result.column === undefined
        ? `Line ${result.line}: `
        : `Line ${result.line}, column ${result.column}: `;
  return `${position}${result.message}`;
}

/** Reads a file and checks its size and syntax. */
export async function checkFile(file: File): Promise<TurtleCheckResult & { tooLarge?: boolean }> {
  if (file.size > MAX_FILE_BYTES) {
    return { ok: false, tooLarge: true, message: 'The file is larger than 5 MB.' };
  }
  const text = await file.text();
  return checkTurtle(text);
}
