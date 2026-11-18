import { reactive } from 'vue';
import type { ValidationWarning } from './types';

export interface Notification {
  id: number;
  kind: 'success' | 'error';
  message: string;
  detail?: string;
  warnings?: ValidationWarning[];
}

/** Notifications shown at the top of every screen, kept across navigation until closed. */
export const notifications = reactive<{ items: Notification[] }>({ items: [] });
let next = 1;

export function notifySuccess(message: string, warnings: ValidationWarning[] = []): void {
  notifications.items.push({ id: next++, kind: 'success', message, warnings });
}

export function notifyError(message: string, detail?: string): void {
  notifications.items.push({ id: next++, kind: 'error', message, detail });
}

export function dismiss(id: number): void {
  notifications.items = notifications.items.filter((n) => n.id !== id);
}

export function clearNotifications(): void {
  notifications.items = [];
}
