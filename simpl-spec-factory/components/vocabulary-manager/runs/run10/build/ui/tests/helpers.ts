import { createMemoryHistory, createRouter } from 'vue-router';
import { routes } from '../src/router';
import { setSession } from '../src/auth';

export function signInAs(roles: string[]): void {
  setSession({
    token: async () => 't',
    roles: () => roles,
    userName: () => 'ga-test',
    logout: () => undefined,
  });
}

export async function routerAt(path: string) {
  const router = createRouter({ history: createMemoryHistory(), routes });
  await router.push(path);
  await router.isReady();
  return router;
}

export function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}
