import { inject, reactive, type InjectionKey } from 'vue';
import type { VocabularyApi, ValidationWarning } from './api';
import { ADMIN_ROLE, type Session } from './auth';

/** What every view needs: the API client, the session and a one-shot notice for the next page. */
export interface AppContext {
  api: VocabularyApi;
  session: Session;
  isAdmin: () => boolean;
  notice: { message: string; warnings: ValidationWarning[] } | null;
}

export const APP_CONTEXT: InjectionKey<AppContext> = Symbol('app-context');

export function createContext(api: VocabularyApi, session: Session): AppContext {
  return reactive({
    api,
    session,
    isAdmin: () => session.roles().includes(ADMIN_ROLE),
    notice: null,
  }) as AppContext;
}

export function useApp(): AppContext {
  const context = inject(APP_CONTEXT);
  if (!context) {
    throw new Error('App context missing');
  }
  return context;
}
