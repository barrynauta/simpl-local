import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import VocabularyNewVersion from "../src/views/VocabularyNewVersion.vue";
import { flush, testRouter } from "./helpers";
import * as api from "../src/api";

vi.mock("../src/auth", () => ({ isAdmin: () => true, session: () => null }));
vi.mock("../src/api", async (original) => {
  const real = await original<typeof import("../src/api")>();
  return { ...real, getVocabulary: vi.fn(), getContent: vi.fn(), createVersion: vi.fn() };
});

describe("VocabularyNewVersion", () => {
  it("pre-fills the form and sends the current content when no file is chosen (D33, D46)", async () => {
    vi.mocked(api.getVocabulary).mockResolvedValue({
      href: "",
      contentHref: "",
      name: "Health",
      version: 3,
      description: "Health terms",
      status: "ACTIVE",
      creationDate: "",
      lastUpdateDate: "",
    });
    vi.mocked(api.getContent).mockResolvedValue(new Blob(["@prefix ex: <http://ex.org/> ."]));
    vi.mocked(api.createVersion).mockResolvedValue({
      href: "",
      name: "Health",
      version: 4,
      description: "Health terms",
      changelog: "x",
      status: "ACTIVE",
      creationDate: "",
      lastUpdateDate: "",
      validationWarnings: [],
    });
    const wrapper = mount(VocabularyNewVersion, {
      props: { name: "Health" },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect((wrapper.find("#field-description").element as HTMLTextAreaElement).value).toBe("Health terms");
    expect(wrapper.text()).toContain("Health.ttl (current content)");
    expect(wrapper.text()).toContain(
      "Submitting creates version 4 and deprecates every older ACTIVE version.",
    );
    await wrapper.find("#field-changelog").setValue("Fixed labels");
    await wrapper.find("form").trigger("submit");
    await flush();
    await flush();
    const sent = vi.mocked(api.createVersion).mock.calls[0][1] as FormData;
    expect(sent.get("changelog")).toBe("Fixed labels");
    expect(sent.get("vocabularyFile")).not.toBeNull();
    expect(sent.get("version")).toBeNull();
  });
});
