import { Parser } from 'n3';
import { MAX_FILE_BYTES, TEXT } from './texts';

/** Result of the browser upload check (D13, D60, D76). */
export interface UploadCheck {
  ok: boolean;
  /** Field error under 'Vocabulary file' (file refused), or null. */
  fieldError: string | null;
  /** Message of the syntax check: success text or the syntax error with its line. */
  message: string;
}

const LINE_PATTERN = /line (\d+)/i;

/** Parses Turtle with N3.js and reports the first syntax error with its line (and column when given). */
export function checkTurtleText(text: string): UploadCheck {
  try {
    new Parser({ format: 'text/turtle' }).parse(text);
    return { ok: true, fieldError: null, message: TEXT.syntaxPassed };
  } catch (error) {
    const err = error as Error & { context?: { line?: number; column?: number } };
    const line = err.context?.line ?? Number(LINE_PATTERN.exec(err.message)?.[1] ?? NaN);
    const column = err.context?.column;
    let where = Number.isFinite(line) ? `Line ${line}` : 'Syntax error';
    if (Number.isFinite(line) && column !== undefined && Number.isFinite(column)) {
      where += `, column ${column}`;
    }
    return { ok: false, fieldError: null, message: `${where}: ${err.message}` };
  }
}

export async function checkTurtleFile(file: File): Promise<UploadCheck> {
  if (file.size > MAX_FILE_BYTES) {
    return { ok: false, fieldError: TEXT.fileTooLarge, message: '' };
  }
  return checkTurtleText(await file.text());
}
