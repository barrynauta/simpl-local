import { Parser } from 'n3';
import { FILE_TOO_LARGE, SYNTAX_PASSED, UPLOAD_LIMIT_BYTES } from './messages';

/** Form field rules of D37, checked in the browser before submit. */
const NAME_PATTERN = /^[A-Z][a-zA-Z0-9]*$/;
export const MAX_TEXT = 1000;

export function nameError(name: string): string | null {
  if (!name) {
    return 'Name is required.';
  }
  if (name.length < 3 || name.length > 64 || !NAME_PATTERN.test(name)) {
    return 'Name must have 3 to 64 characters, start with an upper-case letter and contain only letters and digits.';
  }
  return null;
}

export function textError(label: string, value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return `${label} is required.`;
  }
  if (trimmed.length > MAX_TEXT) {
    return `${label} must have at most ${MAX_TEXT} characters.`;
  }
  return null;
}

export interface UploadCheck {
  ok: boolean;
  message: string;
  line?: number;
  column?: number;
}

/** Syntax check of a chosen file in the browser (D13, D60): line always, column when the parser gives one. */
export function checkTurtle(text: string, size: number): UploadCheck {
  if (size > UPLOAD_LIMIT_BYTES) {
    return { ok: false, message: FILE_TOO_LARGE };
  }
  try {
    new Parser({ baseIRI: 'https://authority.example/vocabulary-manager/content/Upload/versions/1' }).parse(
      text,
    );
    return { ok: true, message: SYNTAX_PASSED };
  } catch (error) {
    const err = error as Error & { context?: { line?: number; column?: number } };
    const line = err.context?.line;
    const column = err.context?.column;
    const where = line
      ? `Syntax error on line ${line}${column ? `, column ${column}` : ''}: `
      : 'Syntax error: ';
    return { ok: false, message: where + err.message, line, column };
  }
}
