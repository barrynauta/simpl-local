import { createRouter, createMemoryHistory } from 'vue-router';
import { setSession } from '../src/auth';
import { routes } from '../src/router';

export function useSession(admin: boolean): void {
  setSession({
    token: async () => 'token',
    isAdmin: () => admin,
    username: () => (admin ? 'ga-admin' : 'ga-viewer'),
    logout: () => undefined,
  });
}

export function testRouter() {
  return createRouter({ history: createMemoryHistory(), routes });
}

export const flush = () => new Promise((resolve) => setTimeout(resolve, 0));
