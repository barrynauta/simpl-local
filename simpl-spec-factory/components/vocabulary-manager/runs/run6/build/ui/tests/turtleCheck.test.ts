import { describe, expect, it } from "vitest";
import { checkFile, checkTurtle } from "../src/turtleCheck";
import { TEXTS } from "../src/texts";

describe("browser syntax check (D13, D60)", () => {
  it("passes valid Turtle", () => {
    const result = checkTurtle('@prefix ex: <http://e/> .\nex:a ex:b "c" .\n');
    expect(result).toEqual({ ok: true, message: TEXTS.syntaxPassed });
  });
  it("reports the line of the first error", () => {
    const result = checkTurtle("@prefix ex: <http://e/> .\nex:a ex:b .\n");
    expect(result.ok).toBe(false);
    expect(result.line).toBe(2);
    expect(result.message).toContain("line 2");
  });
  it("refuses files over 5 MB", async () => {
    const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], "big.ttl");
    const result = await checkFile(big);
    expect(result).toEqual({ ok: false, message: TEXTS.tooLarge });
  });
  it("checks small files by content", async () => {
    const file = new File(['@prefix ex: <http://e/> .\nex:a ex:b "c" .\n'], "ok.ttl");
    expect((await checkFile(file)).ok).toBe(true);
  });
});
