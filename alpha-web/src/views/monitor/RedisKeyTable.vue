<script setup lang="ts">
import DeleteOutlined from '@ant-design/icons-vue/lib/icons/DeleteOutlined'
import EyeOutlined from '@ant-design/icons-vue/lib/icons/EyeOutlined'

import TableActionMenu from '@/components/TableActionMenu.vue'
import type { RedisKeyMetadata } from '@/service/monitor/index'

defineProps<{
    rows: RedisKeyMetadata[]
    loading: boolean
}>()

const emit = defineEmits<{
    inspect: [row: RedisKeyMetadata]
    remove: [row: RedisKeyMetadata]
}>()

function displayLevelLabel(level: RedisKeyMetadata['displayLevel']) {
    return { HIDDEN: '完全隐藏', MASKED: '已脱敏', PLAIN: '明文' }[level]
}

function displayLevelColor(level: RedisKeyMetadata['displayLevel']) {
    return { HIDDEN: 'red', MASKED: 'orange', PLAIN: 'green' }[level]
}
</script>

<template>
    <a-table
        row-key="key"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1160 }"
    >
        <a-table-column title="键名" data-index="key" width="420">
            <template #default="{ text }">
                <a-tooltip :title="text" overlay-class-name="redis-key-tooltip">
                    <span class="redis-key-cell">{{ text }}</span>
                </a-tooltip>
            </template>
        </a-table-column>
        <a-table-column title="分类" data-index="category" width="160" />
        <a-table-column title="类型" data-index="type" width="120" />
        <a-table-column title="TTL（秒）" data-index="ttlSeconds" width="130" />
        <a-table-column
            title="大小估计（字节）"
            data-index="sizeBytes"
            width="170"
        />
        <a-table-column title="值预览" data-index="value" width="220">
            <template #default="{ record }">
                <a-tooltip
                    :title="
                        record.displayLevel === 'HIDDEN'
                            ? undefined
                            : record.value || ''
                    "
                >
                    <span class="redis-value-cell">{{
                        record.value || '-'
                    }}</span>
                </a-tooltip>
                <a-tag
                    class="ml-2"
                    :color="displayLevelColor(record.displayLevel)"
                >
                    {{ displayLevelLabel(record.displayLevel) }}
                </a-tag>
            </template>
        </a-table-column>
        <a-table-column title="操作" width="88" fixed="right" align="center">
            <template #default="{ record }">
                <TableActionMenu aria-label="Redis 键操作">
                    <a-menu-item
                        key="metadata"
                        @click="emit('inspect', record)"
                    >
                        <EyeOutlined />元数据
                    </a-menu-item>
                    <a-menu-item
                        key="delete"
                        v-permission="'monitor:redis:delete'"
                        data-testid="delete-redis-key"
                        danger
                        @click="emit('remove', record)"
                        ><DeleteOutlined />删除</a-menu-item
                    >
                </TableActionMenu>
            </template>
        </a-table-column>
    </a-table>
</template>
