<script setup lang="ts">
import { MoreOutlined } from '@ant-design/icons-vue'

withDefaults(
    defineProps<{
        ariaLabel?: string
    }>(),
    {
        ariaLabel: '行操作',
    },
)
</script>

<template>
    <a-dropdown placement="bottomRight" :trigger="['click']">
        <a-button type="link" size="small" :aria-label="ariaLabel">
            <MoreOutlined />操作
        </a-button>
        <template #overlay>
            <a-menu class="table-action-menu">
                <slot />
            </a-menu>
        </template>
    </a-dropdown>
</template>
