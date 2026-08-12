<script setup lang="ts">
import { useSiteStore } from '@/store/modules/site';
import { $t } from '@/locales';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** Whether to show the title */
  showTitle?: boolean;
}

withDefaults(defineProps<Props>(), {
  showTitle: true
});

const siteStore = useSiteStore();
</script>

<template>
  <RouterLink to="/" class="w-full flex-center nowrap-hidden">
    <img
      v-if="siteStore.siteLogoUrl"
      :src="siteStore.siteLogoUrl"
      :alt="siteStore.siteName"
      class="size-32px object-contain"
    />
    <SystemLogo v-else class="size-32px" />
    <h2 v-show="showTitle" class="pl-8px text-16px text-primary font-bold transition duration-300 ease-in-out">
      {{ siteStore.siteName || $t('system.title') }}
    </h2>
  </RouterLink>
</template>

<style scoped></style>
