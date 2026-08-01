<script setup lang="ts">
import { SettingOutlined } from '@ant-design/icons-vue'
import { computed, ref, watch } from 'vue'

export interface TableColumnSettingItem {
    field: string
    title: string
    visible: boolean
    fixed?: 'left' | 'right'
    align?: 'left' | 'center' | 'right'
}

const props = withDefaults(
    defineProps<{
        modelValue: TableColumnSettingItem[]
        storageKey: string
        control?: 'fixed' | 'align' | 'both'
    }>(),
    { control: 'fixed' },
)

const emit = defineEmits<{
    'update:modelValue': [value: TableColumnSettingItem[]]
    change: [value: TableColumnSettingItem[]]
}>()

const open = ref(false)
const draggingField = ref<string>()
const activeControl = ref<'fixed' | 'align'>(
    props.control === 'fixed' ? 'fixed' : 'align',
)

const visibleCount = computed(
    () => props.modelValue.filter((column) => column.visible).length,
)
const allVisible = computed(
    () => visibleCount.value === props.modelValue.length,
)
const partiallyVisible = computed(
    () => visibleCount.value > 0 && !allVisible.value,
)

function persist(columns: TableColumnSettingItem[]) {
    const orderedColumns =
        props.control === 'fixed' || props.control === 'both'
            ? normalizeFixedOrder(columns)
            : columns
    const next = orderedColumns.map((column, order) => ({ ...column, order }))
    emit('update:modelValue', next)
    emit('change', next)
    window.localStorage.setItem(
        props.storageKey,
        JSON.stringify({ version: 1, columns: next }),
    )
}

function normalizeFixedOrder(columns: TableColumnSettingItem[]) {
    return [
        ...columns.filter((column) => column.fixed === 'left'),
        ...columns.filter((column) => !column.fixed),
        ...columns.filter((column) => column.fixed === 'right'),
    ]
}

function restore() {
    const defaults = props.modelValue.map((column) => ({ ...column }))
    try {
        const raw = window.localStorage.getItem(props.storageKey)
        if (!raw) return
        const parsed: unknown = JSON.parse(raw)
        if (!parsed || typeof parsed !== 'object') return
        const saved = (parsed as { columns?: unknown }).columns
        if (!Array.isArray(saved)) return
        const byField = new Map(
            saved
                .filter(isStoredColumn)
                .map((column) => [column.field, column]),
        )
        defaults.sort(
            (left, right) =>
                (byField.get(left.field)?.order ?? 999) -
                (byField.get(right.field)?.order ?? 999),
        )
        defaults.forEach((column) => {
            const savedColumn = byField.get(column.field)
            if (!savedColumn) return
            column.visible = savedColumn.visible
            if (
                (props.control === 'align' || props.control === 'both') &&
                savedColumn.align
            ) {
                column.align = savedColumn.align
            }
            if (
                (props.control === 'fixed' || props.control === 'both') &&
                savedColumn.fixed
            ) {
                column.fixed = savedColumn.fixed
            }
        })
        if (defaults.every((column) => !column.visible))
            defaults[0].visible = true
        emit(
            'update:modelValue',
            props.control === 'fixed' || props.control === 'both'
                ? normalizeFixedOrder(defaults)
                : defaults,
        )
    } catch {
        // Invalid local settings are ignored and replaced by page defaults.
    }
}

function isStoredColumn(value: unknown): value is TableColumnSettingItem & {
    order: number
} {
    if (!value || typeof value !== 'object') return false
    const column = value as Partial<TableColumnSettingItem> & {
        order?: unknown
    }
    return (
        typeof column.field === 'string' &&
        typeof column.visible === 'boolean' &&
        typeof column.order === 'number'
    )
}

function toggleAll(checked: boolean) {
    const next = props.modelValue.map((column) => ({
        ...column,
        visible: checked,
    }))
    if (!checked) next[0].visible = true
    persist(next)
}

function toggle(column: TableColumnSettingItem, visible: boolean) {
    if (!visible && visibleCount.value <= 1) return
    persist(
        props.modelValue.map((item) =>
            item.field === column.field ? { ...item, visible } : { ...item },
        ),
    )
}

function cycleFixed(column: TableColumnSettingItem) {
    const fixed =
        column.fixed === undefined
            ? 'left'
            : column.fixed === 'left'
              ? 'right'
              : undefined
    persist(
        props.modelValue.map((item) =>
            item.field === column.field ? { ...item, fixed } : { ...item },
        ),
    )
}

function cycleAlign(column: TableColumnSettingItem) {
    const align =
        column.align === 'left'
            ? 'center'
            : column.align === 'center'
              ? 'right'
              : 'left'
    persist(
        props.modelValue.map((item) =>
            item.field === column.field ? { ...item, align } : { ...item },
        ),
    )
}

function toggleControl() {
    if (props.control !== 'both') return
    activeControl.value = activeControl.value === 'align' ? 'fixed' : 'align'
}

function startDrag(field: string) {
    draggingField.value = field
}

function drop(targetField: string) {
    const sourceField = draggingField.value
    draggingField.value = undefined
    if (!sourceField || sourceField === targetField) return
    const next = props.modelValue.map((column) => ({ ...column }))
    const from = next.findIndex((column) => column.field === sourceField)
    const to = next.findIndex((column) => column.field === targetField)
    if (from < 0 || to < 0) return
    const [column] = next.splice(from, 1)
    next.splice(to, 0, column)
    persist(next)
}

watch(open, (value) => {
    if (value) restore()
})
</script>

<template>
    <a-popover v-model:open="open" trigger="click" placement="bottomRight">
        <template #content>
            <div class="table-column-setting-panel">
                <div class="table-column-setting-header">
                    <a-checkbox
                        :checked="allVisible"
                        :indeterminate="partiallyVisible"
                        @change="toggleAll($event.target.checked)"
                        >全选</a-checkbox
                    >
                    <a-button
                        v-if="control === 'both'"
                        type="link"
                        size="small"
                        @click="toggleControl"
                        >{{
                            activeControl === 'align' ? '对齐' : '固定'
                        }}</a-button
                    >
                    <span v-else>
                        {{ control === 'align' ? '设置对齐' : '点击固定' }}
                    </span>
                </div>
                <div
                    v-for="column in modelValue"
                    :key="column.field"
                    class="table-column-setting-item"
                    draggable="true"
                    @dragstart="startDrag(column.field)"
                    @dragover.prevent
                    @drop="drop(column.field)"
                >
                    <span class="table-column-setting-handle">⋮⋮</span>
                    <a-checkbox
                        :checked="column.visible"
                        @change="toggle(column, $event.target.checked)"
                        >{{ column.title }}</a-checkbox
                    >
                    <a-button
                        v-if="activeControl === 'align'"
                        type="text"
                        size="small"
                        :class="{ 'is-fixed': column.align !== 'left' }"
                        :title="`当前${column.align ?? '左'}对齐，点击切换`"
                        @click="cycleAlign(column)"
                        >{{
                            column.align === 'center'
                                ? '中'
                                : column.align === 'right'
                                  ? '右'
                                  : '左'
                        }}</a-button
                    >
                    <a-button
                        v-else
                        type="text"
                        size="small"
                        :disabled="!column.visible"
                        :class="{ 'is-fixed': column.fixed }"
                        :title="
                            column.fixed === 'left'
                                ? '取消左固定'
                                : column.fixed === 'right'
                                  ? '取消固定'
                                  : '固定到左侧'
                        "
                        @click="cycleFixed(column)"
                        >{{
                            column.fixed === 'right'
                                ? '右'
                                : column.fixed
                                  ? '左'
                                  : '固'
                        }}</a-button
                    >
                </div>
            </div>
        </template>
        <a-button class="table-column-setting-trigger">
            <SettingOutlined />列设置
        </a-button>
    </a-popover>
</template>

<style scoped>
.table-column-setting-panel {
    width: 248px;
}

.table-column-setting-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--alpha-border-soft);
    color: var(--alpha-text-secondary);
    font-size: 12px;
}

.table-column-setting-item {
    display: flex;
    align-items: center;
    min-height: 34px;
    gap: 6px;
    cursor: grab;
}

.table-column-setting-handle {
    color: var(--alpha-text-tertiary);
    letter-spacing: -3px;
}

.table-column-setting-item :deep(.ant-checkbox-wrapper) {
    flex: 1;
}

.table-column-setting-item .is-fixed {
    color: var(--alpha-primary);
}
</style>
