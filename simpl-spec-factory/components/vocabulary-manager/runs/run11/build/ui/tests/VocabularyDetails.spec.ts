import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import VocabularyDetails from "../src/views/VocabularyDetails.vue";
import { flush, testRouter } from "./helpers";
import * as api from "../src/api";

vi.mock("../src/auth", () => ({ isAdmin: () => true, session: () => null }));
vi.mock("../src/api", async (original) => {
  const real = await original<typeof import("../src/api")>();
  return { ...real, getVocabulary: vi.fn(), getAllVersions: vi.fn(), download: vi.fn() };
});

function version(n: number, status: "ACTIVE" | "DEPRECATED") {
  return {
    href: "",
    name: "Health",
    version: n,
    description: `d${n}`,
    changelog: `c${n}`,
    status,
    createdBy: "ga-admin@example.com",
    creationDate: "2026-11-18T09:27:00Z",
    lastUpdateDate: "2026-11-18T09:27:00Z",
  };
}

const vocabulary = {
  href: "",
  contentHref: "",
  name: "Health",
  version: 2,
  description: "d2",
  status: "ACTIVE" as const,
  creationDate: "2026-11-18T09:27:00Z",
  lastUpdateDate: "2026-11-18T09:27:00Z",
};

describe("VocabularyDetails", () => {
  it("warns on a deprecated version and links the latest ACTIVE version (D36, D118)", async () => {
    vi.mocked(api.getVocabulary).mockResolvedValue(vocabulary);
    vi.mocked(api.getAllVersions).mockResolvedValue([version(2, "ACTIVE"), version(1, "DEPRECATED")]);
    const router = testRouter();
    await router.push("/vocabularies/Health?version=1");
    const wrapper = mount(VocabularyDetails, { props: { name: "Health" }, global: { plugins: [router] } });
    await flush();
    expect(wrapper.text()).toContain("This version is deprecated.");
    const links = wrapper.findAll("a").filter((a) => a.text() === "Latest ACTIVE version: 2");
    expect(links.length).toBeGreaterThan(0);
    expect(wrapper.find('[aria-label="Version 1"]').text()).toContain("ga-admin@example.com");
  });

  it("shows no ACTIVE link for a deprecated vocabulary and shows the banner (D47, D64, D115)", async () => {
    vi.mocked(api.getVocabulary).mockResolvedValue({ ...vocabulary, status: "DEPRECATED" });
    vi.mocked(api.getAllVersions).mockResolvedValue([version(2, "ACTIVE"), version(1, "DEPRECATED")]);
    const wrapper = mount(VocabularyDetails, {
      props: { name: "Health" },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(wrapper.find('.banner[role="status"]').text()).toBe(
      "This vocabulary is deprecated. Its use is discouraged.",
    );
    expect(wrapper.text()).not.toContain("Latest ACTIVE version");
  });

  it("shows 'Vocabulary not found.' on 404", async () => {
    vi.mocked(api.getVocabulary).mockImplementation(async () => {
      throw new api.ApiError(404, { detail: "x" });
    });
    const wrapper = mount(VocabularyDetails, {
      props: { name: "Nope" },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toBe("Vocabulary not found.");
    expect(wrapper.text()).toContain("Back to vocabulary list");
  });

  it("shows the history load error with the detail", async () => {
    vi.mocked(api.getVocabulary).mockImplementation(async () => {
      throw new api.ApiError(503, { detail: "Retry later." });
    });
    const wrapper = mount(VocabularyDetails, {
      props: { name: "Health" },
      global: { plugins: [testRouter()] },
    });
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toBe(
      "The version history could not be loaded. Retry later.",
    );
  });
});
