import { createApp } from 'vue';
import './plugins/assets';
import { setupAppVersionNotification, setupDayjs, setupIconifyOffline, setupLoading, setupNProgress } from './plugins';
import { setupStore } from './store';
import { setupRouter } from './router';
import { setupI18n } from './locales';
import App from './App.vue';
import { permission } from './directives/permission';
import { useSiteStore } from './store/modules/site';

async function setupApp() {
  setupLoading();

  setupNProgress();

  setupIconifyOffline();

  setupDayjs();

  const app = createApp(App);

  setupStore(app);

  await setupRouter(app);

  await useSiteStore().refresh();

  setupI18n(app);

  app.directive('permission', permission);

  setupAppVersionNotification();

  app.mount('#app');
}

setupApp();
