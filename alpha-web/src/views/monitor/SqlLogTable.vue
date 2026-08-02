<script setup lang="ts">
import { CopyOutlined } from '@ant-design/icons-vue'

import AlphaTableCard from '@/components/AlphaTableCard.vue'
import type { SqlLogEntry } from '@/service/monitor/index'
import { parseDateTime } from '@/utils/dateTime'

defineProps<{
    rows: SqlLogEntry[]
    loading: boolean
}>()

const emit = defineEmits<{
    inspect: [row: SqlLogEntry]
    copy: [row: SqlLogEntry]
}>()

function formatTime(value: string) {
    return new Intl.DateTimeFormat('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
    }).format(parseDateTime(value))
}
</script>

<template>
    <AlphaTableCard class="sql-table-wrap" :loading="loading">
        <a-table
            row-key="id"
            :data-source="rows"
            :loading="loading"
            :pagination="false"
            table-layout="fixed"
            :scroll="{ x: 1500 }"
        >
            <a-table-column title="时间" data-index="createdAt" width="110">
                <template #default="{ text }">{{ formatTime(text) }}</template>
            </a-table-column>
            <a-table-column
                title="类型"
                data-index="sqlCommandType"
                width="100"
            >
                <template #default="{ text }"
                    ><a-tag>{{ text }}</a-tag></template
                >
            </a-table-column>
            <a-table-column title="耗时" data-index="elapsedMs" width="110">
                <template #default="{ record }">
                    <a-tag :color="record.slow ? 'red' : 'green'"
                        >{{ record.elapsedMs }} ms</a-tag
                    >
                </template>
            </a-table-column>
            <a-table-column title="表名" data-index="tableName" width="160">
                <template #default="{ text }">{{ text || '-' }}</template>
            </a-table-column>
            <a-table-column title="结果" data-index="resultSize" :width="180">
                <template #default="{ text }">{{ text ?? '-' }}</template>
            </a-table-column>
            <a-table-column title="traceId" data-index="traceId" width="220">
                <template #default="{ text }"
                    ><span class="sql-trace-cell">{{
                        text || '-'
                    }}</span></template
                >
            </a-table-column>
            <a-table-column title="SQL" data-index="sql" :width="360">
                <template #default="{ record }">
                    <div class="sql-preview-cell">
                        <code class="sql-preview-text" :title="record.sql">{{
                            record.sql
                        }}</code>
                        <a-button
                            type="text"
                            size="small"
                            title="查看 SQL"
                            aria-label="查看 SQL"
                            @click="emit('inspect', record)"
                            >查看</a-button
                        >
                        <a-button
                            type="text"
                            size="small"
                            title="复制 SQL"
                            aria-label="复制 SQL"
                            @click="emit('copy', record)"
                            ><CopyOutlined
                        /></a-button>
                    </div>
                </template>
            </a-table-column>
            <a-table-column title="Mapper" data-index="statementId" width="260">
                <template #default="{ text }"
                    ><span class="sql-statement-cell">{{
                        text
                    }}</span></template
                >
            </a-table-column>
        </a-table>
    </AlphaTableCard>
</template>

<style scoped>
.sql-table-wrap {
    width: 100%;
}

.sql-table-wrap :deep(.ant-table-content) {
    overflow-x: auto !important;
}
</style>
