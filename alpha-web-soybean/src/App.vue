<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ConfigProvider } from 'ant-design-vue';
import type { WatermarkProps } from 'ant-design-vue';
import { useAppStore } from './store/modules/app';
import { useThemeStore } from './store/modules/theme';
import { useAuthStore } from './store/modules/auth';
import { useSiteStore } from './store/modules/site';
import { antdLocales } from './locales/antd';

defineOptions({
  name: 'App'
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const authStore = useAuthStore();
const siteStore = useSiteStore();
const route = useRoute();

const antdLocale = computed(() => {
  return antdLocales[appStore.locale];
});

const watermarkProps = computed(() => {
  const text =
    siteStore.watermarkType === 'username'
      ? authStore.profile?.nickname || authStore.profile?.username || authStore.userInfo.userName
      : siteStore.watermarkContent;
  const props: WatermarkProps = {
    content: text,
    width: siteStore.watermarkGap,
    height: siteStore.watermarkGap,
    gap: [siteStore.watermarkGap, siteStore.watermarkGap],
    font: { fontSize: siteStore.watermarkFontSize, color: `rgba(100, 116, 139, ${siteStore.watermarkOpacity})` },
    offset: [siteStore.watermarkGap / 2, siteStore.watermarkGap / 2],
    rotate: -15,
    zIndex: 9999
  };

  return props;
});

const isLoginRoute = computed(() => route.path === '/login' || route.path.startsWith('/login/'));
</script>

<template>
  <ConfigProvider :theme="themeStore.antdTheme" :locale="antdLocale">
    <AppProvider>
      <RouterView class="bg-layout" />
      <AWatermark
        v-if="!isLoginRoute && siteStore.watermarkEnabled && watermarkProps.content"
        v-bind="watermarkProps"
        class="pointer-events-none size-full absolute-lt!"
      />
    </AppProvider>
  </ConfigProvider>
</template>

<style scoped></style>
