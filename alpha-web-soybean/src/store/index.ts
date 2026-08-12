import type { App } from 'vue';
import { createPinia } from 'pinia';
import { resetSetupStore } from './plugins';

/** Setup Vue store plugin pinia */
export const pinia = createPinia();

pinia.use(resetSetupStore);

export function setupStore(app: App) {
  app.use(pinia);
}
