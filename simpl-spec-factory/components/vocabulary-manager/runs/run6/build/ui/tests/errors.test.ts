import { describe, expect, it } from "vitest";
import { ApiError } from "../src/api";
import { describeError } from "../src/errors";
import { TEXTS } from "../src/texts";

describe("describeError (D18, D24, D60)", () => {
  it("maps 403 to the authorization message", () => {
    expect(describeError(new ApiError(403, { detail: "x" }))).toEqual({
      message: TEXTS.notAuthorized,
      detail: "",
    });
  });
  it("maps 500 and 503 to the generic message with the detail", () => {
    expect(describeError(new ApiError(503, { detail: "down" }))).toEqual({
      message: TEXTS.genericError,
      detail: "down",
    });
    expect(describeError(new ApiError(500, {})).message).toBe(TEXTS.genericError);
  });
  it("shows the problem detail for other errors", () => {
    expect(describeError(new ApiError(409, { detail: "exists" })).message).toBe("exists");
    expect(describeError(new ApiError(413, { title: "Payload Too Large" })).message).toBe(
      "Payload Too Large",
    );
  });
  it("handles network errors", () => {
    expect(describeError(new Error("offline"))).toEqual({ message: TEXTS.genericError, detail: "offline" });
  });
});
