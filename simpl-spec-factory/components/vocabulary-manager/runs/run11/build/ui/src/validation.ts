import { Parser } from "n3";
import { MAX_FILE_BYTES, TEXT } from "./texts";

/** Field rules of D37. Returns the error text or an empty string. */
export function nameError(value: string): string {
  if (value.length === 0) return "Name is required.";
  if (value.length < 3 || value.length > 64) return "Name must have 3 to 64 characters.";
  if (!/^[A-Z][a-zA-Z0-9]*$/.test(value)) {
    return "Name must start with an upper-case letter and contain only letters and digits.";
  }
  return "";
}

export function textError(label: string, value: string): string {
  const trimmed = value.trim();
  if (trimmed.length === 0) return `${label} is required.`;
  if ([...trimmed].length > 1000) return `${label} must have at most 1000 characters.`;
  return "";
}

export interface UploadCheck {
  ok: boolean;
  message: string;
  fieldError: string;
}

/** The upload check in the browser (D13, D60, D76): size, then Turtle syntax with N3.js. */
export function checkTurtle(text: string, size: number): UploadCheck {
  if (size > MAX_FILE_BYTES) {
    return { ok: false, message: "", fieldError: TEXT.fileTooLarge };
  }
  try {
    new Parser({ format: "text/turtle" }).parse(text);
    return { ok: true, message: TEXT.syntaxPassed, fieldError: "" };
  } catch (error) {
    const e = error as Error & { context?: { line?: number } };
    const line = e.context?.line;
    const where = line ? ` (line ${line})` : "";
    return { ok: false, message: `Syntax error${where}: ${e.message}`, fieldError: "" };
  }
}

export async function checkFile(file: File): Promise<UploadCheck> {
  if (file.size > MAX_FILE_BYTES) {
    return checkTurtle("", file.size);
  }
  return checkTurtle(await readText(file), file.size);
}

function readText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ""));
    reader.onerror = () => reject(reader.error ?? new Error("The file could not be read."));
    reader.readAsText(file);
  });
}
