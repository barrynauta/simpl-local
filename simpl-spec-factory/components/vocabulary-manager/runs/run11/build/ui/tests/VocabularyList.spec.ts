import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import VocabularyList from "../src/views/VocabularyList.vue";
import { flush, testRouter } from "./helpers";
import * as api from "../src/api";

vi.mock("../src/auth", () => ({ isAdmin: () => true, session: () => null }));
vi.mock("../src/api", async (original) => {
  const real = await original<typeof import("../src/api")>();
  return { ...real, listVocabularies: vi.fn(), setStatus: vi.fn(), download: vi.fn() };
});

const row = {
  href: "/vocabularies/Health",
  contentHref: "x",
  name: "Health",
  version: 2,
  description: "Health terms",
  status: "DEPRECATED" as const,
  createdBy: "ga-admin@example.com",
  creationDate: "2026-11-18T09:27:00Z",
  lastUpdateDate: "2026-11-19T10:00:00Z",
};

describe("VocabularyList", () => {
  it("shows the decided columns, the deprecation icon and the page text", async () => {
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [{ ...row }],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    const headers = wrapper.findAll("th").map((h) => h.text().replace(/[▲▼]/g, "").trim());
    expect(headers).toEqual([
      "Name",
      "Status",
      "Description",
      "Creator",
      "Date created",
      "Last updated",
      "Actions",
    ]);
    expect(wrapper.find('[role="img"]').attributes("aria-label")).toBe(
      "This vocabulary is deprecated. Its use is discouraged.",
    );
    expect(wrapper.text()).toContain("2026-11-18 09:27 UTC");
    expect(wrapper.text()).toContain("Page 1 of 1");
    expect(wrapper.text()).toContain("New Vocabulary +");
  });

  it("reverses the active sort column on click (D50)", async () => {
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 0,
      totalPages: 0,
      items: [],
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    expect(wrapper.text()).toContain("No vocabularies found.");
    await wrapper.find("button.sort").trigger("click");
    await flush();
    const query = vi.mocked(api.listVocabularies).mock.calls.at(-1)?.[0] as URLSearchParams;
    expect(query.get("sort")).toBe("-name");
  });

  it("shows one alert with the decided text and the problem detail on a load error", async () => {
    vi.mocked(api.listVocabularies).mockImplementation(async () => {
      throw new api.ApiError(500, { detail: "Internal Server Error" });
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    const alerts = wrapper.findAll('[role="alert"]');
    expect(alerts.length).toBe(1);
    expect(alerts[0].text()).toBe("The vocabulary list could not be loaded. Internal Server Error");
  });

  it("changes the status from the actions menu", async () => {
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [{ ...row }],
    });
    vi.mocked(api.setStatus).mockResolvedValue({ ...row, status: "ACTIVE" });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    await wrapper.find('button[aria-label="Actions for Health"]').trigger("click");
    const activate = wrapper.findAll('[role="menuitem"]').find((b) => b.text() === "Activate");
    await activate?.trigger("click");
    await flush();
    expect(wrapper.find('[role="status"]').text()).toBe("Status of Health changed to Active.");
  });

  it("explains a failed status change with a 404", async () => {
    vi.mocked(api.listVocabularies).mockResolvedValue({
      page: 1,
      pageSize: 20,
      total: 1,
      totalPages: 1,
      items: [{ ...row }],
    });
    vi.mocked(api.setStatus).mockImplementation(async () => {
      throw new api.ApiError(404, { detail: "gone" });
    });
    const wrapper = mount(VocabularyList, { global: { plugins: [testRouter()] } });
    await flush();
    await wrapper.find('button[aria-label="Actions for Health"]').trigger("click");
    await wrapper
      .findAll('[role="menuitem"]')
      .find((b) => b.text() === "Activate")
      ?.trigger("click");
    await flush();
    expect(wrapper.find('[role="alert"]').text()).toContain(
      "Vocabulary not found. The status was not changed.",
    );
  });
});
