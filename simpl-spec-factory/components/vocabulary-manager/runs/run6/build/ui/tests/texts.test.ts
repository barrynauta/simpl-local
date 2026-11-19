import { describe, expect, it } from "vitest";
import { TEXTS } from "../src/texts";

describe("decided texts", () => {
  it("builds the parameterised texts", () => {
    expect(TEXTS.versionCreated(2, "Health")).toBe("Version 2 of Health was created.");
    expect(TEXTS.statusChanged("Health", "Deprecated")).toBe("Status of Health changed to Deprecated.");
    expect(TEXTS.submitCreates(3)).toBe(
      "Submitting creates version 3 and deprecates every older ACTIVE version.",
    );
    expect(TEXTS.latestActive(4)).toBe("Latest ACTIVE version: 4");
    expect(TEXTS.currentContent("Health")).toBe("Health.ttl (current content)");
  });
});
