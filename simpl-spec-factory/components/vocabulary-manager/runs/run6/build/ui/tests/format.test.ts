import { describe, expect, it } from "vitest";
import { formatUtc, nameError, textError } from "../src/format";

describe("formatUtc (D60)", () => {
  it("formats UTC to the minute", () => {
    expect(formatUtc("2026-11-18T09:27:41Z")).toBe("2026-11-18 09:27 UTC");
  });
  it("keeps an unparsable value and empties a missing one", () => {
    expect(formatUtc("not a date")).toBe("not a date");
    expect(formatUtc(undefined)).toBe("");
  });
});

describe("field rules (D37)", () => {
  it("checks names", () => {
    expect(nameError("")).toBe("Name is required.");
    expect(nameError("ab")).not.toBeNull();
    expect(nameError("lowerCase")).not.toBeNull();
    expect(nameError("Health1")).toBeNull();
    expect(nameError("A" + "b".repeat(64))).not.toBeNull();
  });
  it("checks texts after trimming", () => {
    expect(textError("Description", "   ")).toBe("Description is required.");
    expect(textError("Description", "x".repeat(1001))).toBe("Description must be at most 1000 characters.");
    expect(textError("Change log", " ok ")).toBeNull();
  });
});
