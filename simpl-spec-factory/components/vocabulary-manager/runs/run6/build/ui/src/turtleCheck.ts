import { Parser } from "n3";
import { MAX_UPLOAD_BYTES, TEXTS } from "./texts";

/** Result of the browser-side check of an uploaded file (decision D13). */
export interface FileCheck {
  ok: boolean;
  message: string;
  line?: number;
  column?: number;
}

/**
 * Parses Turtle in the browser with the N3.js library and reports the first syntax error with its line (and
 * column when the parser gives one). Semantic validation stays on the server.
 */
export function checkTurtle(text: string): FileCheck {
  try {
    new Parser({ format: "text/turtle" }).parse(text);
    return { ok: true, message: TEXTS.syntaxPassed };
  } catch (e) {
    const error = e as Error & { context?: { line?: number; column?: number } };
    const line = error.context?.line ?? lineFromMessage(error.message);
    const column = error.context?.column;
    const where = line ? ` (line ${line}${column ? `, column ${column}` : ""})` : "";
    return { ok: false, message: `Syntax error${where}: ${error.message}`, line, column };
  }
}

function lineFromMessage(message: string): number | undefined {
  const match = /line (\d+)/i.exec(message);
  return match ? Number(match[1]) : undefined;
}

/** Checks the size, then the syntax, of a chosen file. */
export async function checkFile(file: File): Promise<FileCheck> {
  if (file.size > MAX_UPLOAD_BYTES) {
    return { ok: false, message: TEXTS.tooLarge };
  }
  return checkTurtle(await file.text());
}
