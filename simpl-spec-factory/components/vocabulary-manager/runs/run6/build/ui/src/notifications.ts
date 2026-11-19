import { reactive } from "vue";
import type { ValidationWarning } from "./api";

/** Messages that survive a route change (success after create, decision D14). */
export const notice = reactive<{ success: string; warnings: ValidationWarning[] }>({
  success: "",
  warnings: [],
});

export function setNotice(success: string, warnings: ValidationWarning[] = []): void {
  notice.success = success;
  notice.warnings = warnings;
}

export function clearNotice(): void {
  notice.success = "";
  notice.warnings = [];
}
