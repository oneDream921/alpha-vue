<script setup lang="ts">
import { computed } from 'vue'

import type { EnabledDictItem } from '@/service/system/index'
import { dictValueTokens, type DictValue } from '@/utils/dicts'

const props = withDefaults(
    defineProps<{
        options: EnabledDictItem[]
        value?: DictValue
        showValue?: boolean
        separator?: string
    }>(),
    {
        value: undefined,
        showValue: true,
        separator: ',',
    },
)

const values = computed(() => dictValueTokens(props.value, props.separator))
const matched = computed(() =>
    props.options.filter((option) => values.value.includes(option.value)),
)
const unmatched = computed(() =>
    values.value.filter(
        (value) => !props.options.some((option) => option.value === value),
    ),
)
</script>

<template>
    <a-space
        v-if="matched.length || (showValue && unmatched.length)"
        size="small"
    >
        <a-tag
            v-for="item in matched"
            :key="item.value"
            :color="item.isDefault === 1 ? 'blue' : undefined"
        >
            {{ item.label }}
        </a-tag>
        <span v-if="showValue && unmatched.length" class="dict-tag-unmatched">
            {{ unmatched.join(' ') }}
        </span>
    </a-space>
</template>
