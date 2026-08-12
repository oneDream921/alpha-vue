<script setup lang="ts">
import DeleteOutlined from '@ant-design/icons-vue/lib/icons/DeleteOutlined';
import EyeOutlined from '@ant-design/icons-vue/lib/icons/EyeOutlined';
import type { RedisKeyMetadata } from '@/service/api/monitor';
import TableActionMenu from '@/components/TableActionMenu.vue';

defineProps<{
  rows: RedisKeyMetadata[];
  loading: boolean;
}>();

const emit = defineEmits<{
  inspect: [row: RedisKeyMetadata];
  remove: [row: RedisKeyMetadata];
}>();

function displayLevelLabel(level: RedisKeyMetadata['displayLevel']) {
  return { HIDDEN: '完全隐藏', MASKED: '已脱敏', PLAIN: '明文' }[level];
}

function displayLevelColor(level: RedisKeyMetadata['displayLevel']) {
  return { HIDDEN: 'red', MASKED: 'orange', PLAIN: 'green' }[level];
}
</script>

<template>
  <ATable row-key="key" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1160 }">
    <ATableColumn title="键名" data-index="key" width="420">
      <template #default="{ text }">
        <ATooltip :title="text" overlay-class-name="redis-key-tooltip">
          <span class="redis-key-cell">{{ text }}</span>
        </ATooltip>
      </template>
    </ATableColumn>
    <ATableColumn title="分类" data-index="category" width="160" />
    <ATableColumn title="类型" data-index="type" width="120" />
    <ATableColumn title="TTL（秒）" data-index="ttlSeconds" width="130" />
    <ATableColumn title="大小估计（字节）" data-index="sizeBytes" width="170" />
    <ATableColumn title="值预览" data-index="value" width="220">
      <template #default="{ record }">
        <ATooltip :title="record.displayLevel === 'HIDDEN' ? undefined : record.value || ''">
          <span class="redis-value-cell">{{ record.value || '-' }}</span>
        </ATooltip>
        <ATag class="ml-2" :color="displayLevelColor(record.displayLevel)">
          {{ displayLevelLabel(record.displayLevel) }}
        </ATag>
      </template>
    </ATableColumn>
    <ATableColumn title="操作" width="88" fixed="right" align="center">
      <template #default="{ record }">
        <TableActionMenu aria-label="Redis 键操作">
          <AMenuItem key="metadata" @click="emit('inspect', record)">
            <EyeOutlined />
            元数据
          </AMenuItem>
          <AMenuItem
            key="delete"
            v-permission="'monitor:redis:delete'"
            data-testid="delete-redis-key"
            danger
            @click="emit('remove', record)"
          >
            <DeleteOutlined />
            删除
          </AMenuItem>
        </TableActionMenu>
      </template>
    </ATableColumn>
  </ATable>
</template>
