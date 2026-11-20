import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import VocabularyCreate from "../src/views/VocabularyCreate.vue";
import { flush, testRouter } from "./helpers";
import * as api from "../src/api";

let admin = true;
vi.mock("../src/auth", () => ({ isAdmin: () => admin, session: () => null }));
vi.mock("../src/api", async (original) => {
  const real = await original<typeof import("../src/api")>();
  return { ...real, createVocabulary: vi.fn() };
});

describe("VocabularyCreate", () => {
  it("has the decided labels with required markers and no change log field", () => {
    const wrapper = mount(VocabularyCreate, { global: { plugins: [testRouter()] } });
    const labels = wrapper.findAll("label").map((l) => l.text());
    expect(labels).toEqual(["Name *", "Description *", "Vocabulary file *"]);
    expect(wrapper.find("#field-name").attributes("aria-required")).toBe("true");
    expect(wrapper.text()).not.toContain("Change log");
  });

  it("checks every field on Submit and sends nothing (D62, D101)", async () => {
    const wrapper = mount(VocabularyCreate, { global: { plugins: [testRouter()] }, attachTo: document.body });
    await wrapper.find("form").trigger("submit");
    await flush();
    expect(wrapper.text()).toContain("Name is required.");
    expect(wrapper.text()).toContain("Description is required.");
    expect(wrapper.text()).toContain("Vocabulary file is required.");
    expect(api.createVocabulary).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe("field-name");
    wrapper.unmount();
  });

  it("shows the validation report on 400 (D90, D109)", async () => {
    vi.mocked(api.createVocabulary).mockImplementation(async () => {
      throw new api.ApiError(400, {
        detail: "The input message is incorrect",
        issues: [
          {
            name: "vocabularyFile",
            detail: "Literal value for object property",
            location: "statement 'a b c'",
          },
        ],
      });
    });
    const wrapper = mount(VocabularyCreate, { global: { plugins: [testRouter()] } });
    await wrapper.find("#field-name").setValue("Health");
    await wrapper.find("#field-description").setValue("Terms");
    const input = wrapper.find("#field-file").element as HTMLInputElement;
    const file = new File(["@prefix ex: <http://ex.org/> . ex:a ex:b ex:c ."], "h.ttl", {
      type: "text/turtle",
    });
    Object.defineProperty(input, "files", { value: [file] });
    await wrapper.find("#field-file").trigger("change");
    await flush();
    await flush();
    expect(wrapper.text()).toContain("Syntax check passed.");
    await wrapper.find("form").trigger("submit");
    await flush();
    await flush();
    const report = wrapper.find('[aria-label="Validation report"]');
    expect(report.find('[role="alert"]').text()).toBe("1 error found");
    expect(report.text()).toContain("Literal value for object property");
    const sent = vi.mocked(api.createVocabulary).mock.calls[0][0] as FormData;
    expect(sent.get("changelog")).toBe("Initial version.");
  });

  it("refuses a user without the admin role", () => {
    admin = false;
    const wrapper = mount(VocabularyCreate, { global: { plugins: [testRouter()] } });
    expect(wrapper.text()).toContain("You are not authorized to perform this action.");
    admin = true;
  });
});
