<script setup lang="ts">
import { computed } from 'vue';
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
    width: 120,
    height: 120,
    font: { fontSize: 16, color: `rgba(100, 116, 139, ${siteStore.watermarkOpacity})` },
    offset: [12, 60],
    rotate: -15,
    zIndex: 9999
  };

  return props;
});
</script>

<template>
  <ConfigProvider :theme="themeStore.antdTheme" :locale="antdLocale">
    <AppProvider>
      <RouterView class="bg-layout" />
      <AWatermark
        v-if="siteStore.watermarkEnabled && watermarkProps.content"
        v-bind="watermarkProps"
        class="pointer-events-none size-full absolute-lt!"
      />
    </AppProvider>
  </ConfigProvider>
</template>

<style scoped></style>
