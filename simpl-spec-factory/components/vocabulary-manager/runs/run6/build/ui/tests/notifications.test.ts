import { describe, expect, it } from "vitest";
import { clearNotice, notice, setNotice } from "../src/notifications";

describe("notice store (D14)", () => {
  it("keeps the success text and warnings until cleared", () => {
    setNotice("Version 1 of Health was created.", [{ code: "externalReferenceSkipped", description: "d", location: "l" }]);
    expect(notice.success).toBe("Version 1 of Health was created.");
    expect(notice.warnings).toHaveLength(1);
    clearNotice();
    expect(notice.success).toBe("");
    expect(notice.warnings).toHaveLength(0);
  });
});
