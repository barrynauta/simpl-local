import { describe, expect, it } from "vitest";
import { checkTurtle, nameError, textError } from "../src/validation";
import { count, formatDate } from "../src/texts";

describe("validation", () => {
  it("applies the name rules of D37", () => {
    expect(nameError("Health")).toBe("");
    expect(nameError("he")).not.toBe("");
    expect(nameError("health")).not.toBe("");
    expect(nameError("")).toBe("Name is required.");
  });

  it("applies the text limits", () => {
    expect(textError("Description", "  ")).toBe("Description is required.");
    expect(textError("Description", "a".repeat(1001))).not.toBe("");
  });

  it("reports the line of a syntax error and refuses large files", () => {
    expect(checkTurtle("@prefix ex: <http://ex.org/> .\nex:a ex:b .", 10).message).toContain("line 2");
    expect(checkTurtle("", 6 * 1024 * 1024).fieldError).toBe("The file is larger than 5 MB.");
    expect(checkTurtle("@prefix ex: <http://ex.org/> . ex:a ex:b ex:c .", 10).message).toBe(
      "Syntax check passed.",
    );
  });

  it("formats counts and dates", () => {
    expect(count(1, "error")).toBe("1 error found");
    expect(count(2, "warning")).toBe("2 warnings found");
    expect(formatDate("2026-11-18T09:27:00Z")).toBe("2026-11-18 09:27 UTC");
  });
});
