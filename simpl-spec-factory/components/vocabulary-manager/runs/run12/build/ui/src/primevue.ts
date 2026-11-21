import type { App } from 'vue';
import PrimeVue from 'primevue/config';
import BaseStyle from '@primevue/core/base/style';

/**
 * PrimeVue in unstyled mode with no theme (E6, D167). Even unstyled, PrimeVue injects its small
 * 'base' style element on first use. Turning that loader off keeps the page free of inline
 * styles, so the Content-Security-Policy needs no 'unsafe-inline' (D123). The one rule it
 * carries, .p-hidden-accessible, is in style.css.
 */
export function installPrimeVue(app: App): void {
  BaseStyle.loadCSS = () => ({});
  app.use(PrimeVue, { unstyled: true, theme: 'none' });
}
