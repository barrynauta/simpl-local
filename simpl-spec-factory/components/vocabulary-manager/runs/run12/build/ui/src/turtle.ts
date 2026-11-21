import N3Parser from 'n3/src/N3Parser.js';

export type TurtleCheck =
  { ok: true } | { ok: false; line?: number; column?: number; message: string };

/** D13: parses Turtle in the browser and reports the first syntax error. */
export function checkTurtle(text: string): TurtleCheck {
  try {
    new N3Parser({ format: 'text/turtle' }).parse(text);
    return { ok: true };
  } catch (error) {
    const err = error as Error & { context?: { line?: number; column?: number } };
    return {
      ok: false,
      line: err.context?.line,
      column: err.context?.column,
      message: err.message,
    };
  }
}

/** D60: the line is always shown; the column only when the parser gives one. */
export function describeSyntaxError(check: TurtleCheck): string {
  if (check.ok) return '';
  let where = '';
  if (check.line !== undefined) {
    where = ` at line ${check.line}`;
    if (check.column !== undefined) where += `, column ${check.column}`;
  }
  return `Syntax error${where}: ${check.message}`;
}
