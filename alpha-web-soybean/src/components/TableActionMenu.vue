<script setup lang="ts">
import { MoreOutlined } from '@ant-design/icons-vue';

withDefaults(
  defineProps<{
    ariaLabel?: string;
  }>(),
  {
    ariaLabel: '行操作'
  }
);
</script>

<template>
  <ADropdown placement="bottomRight" :trigger="['click']">
    <AButton type="link" size="small" :aria-label="ariaLabel">
      <MoreOutlined />
      操作
    </AButton>
    <template #overlay>
      <AMenu class="table-action-menu">
        <slot />
      </AMenu>
    </template>
  </ADropdown>
</template>
