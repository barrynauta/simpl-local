import { vi } from 'vitest';

export const apiMock = {
  listVocabularies: vi.fn(),
  getVocabulary: vi.fn(),
  listAllVersions: vi.fn(),
  changeStatus: vi.fn(),
  createVocabulary: vi.fn(),
  createVersion: vi.fn(),
  downloadContent: vi.fn(),
};

export function resetApiMock() {
  Object.values(apiMock).forEach((fn) => fn.mockReset());
}
