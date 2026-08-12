<script setup lang="ts">
import { computed } from 'vue';
import { useSiteStore } from '@/store/modules/site';

defineOptions({
  name: 'GlobalFooter'
});

const siteStore = useSiteStore();
const footerText = computed(() => [siteStore.copyright, siteStore.icp].filter(Boolean).join(' · '));
</script>

<template>
  <DarkModeContainer class="h-full flex-center">
    <a href="https://github.com/honghuangdc/soybean-admin/blob/main/LICENSE" target="_blank" rel="noopener noreferrer">
      {{ footerText }}
    </a>
  </DarkModeContainer>
</template>

<style scoped></style>
