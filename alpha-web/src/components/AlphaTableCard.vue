<script setup lang="ts">
import { useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
    defineProps<{
        loading?: boolean
    }>(),
    { loading: false },
)

const attrs = useAttrs()
</script>

<template>
    <section class="alpha-table-card" v-bind="attrs">
        <div v-if="$slots.toolbar" class="alpha-table-card-toolbar">
            <slot name="toolbar" />
        </div>
        <a-spin :spinning="loading" class="alpha-table-card-spin">
            <div class="alpha-table-card-body">
                <div class="alpha-table-card-table">
                    <slot />
                </div>
            </div>
        </a-spin>
        <div v-if="$slots.footer" class="alpha-table-card-footer">
            <slot name="footer" />
        </div>
    </section>
</template>

<style>
.alpha-table-card {
    box-sizing: border-box;
    width: 100%;
    overflow: hidden;
    border: 1px solid #dfe5ee;
    border-radius: 12px;
    background: var(--alpha-surface);
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.alpha-table-card-toolbar {
    display: block;
    padding: 14px 16px 8px;
}

.alpha-table-card-body {
    min-width: 0;
}

.alpha-table-card-table {
    overflow: hidden;
}

.alpha-table-card-table > .ant-table-wrapper {
    width: 100%;
}

.alpha-table-card-table .ant-table {
    background: transparent;
}

.alpha-table-card-table .ant-table-thead > tr > th {
    background: #fafbfc;
    border-bottom: 1px solid var(--alpha-border-soft);
    color: var(--alpha-text-primary);
    font-weight: 600;
}

.alpha-table-card-table .ant-table-tbody > tr > td {
    border-bottom: 1px solid var(--alpha-border-soft);
}

.alpha-table-card-table .ant-table-tbody > tr:last-child > td {
    border-bottom: 0;
}

.alpha-table-card-footer {
    display: flex;
    justify-content: flex-end;
    padding: 14px 16px 16px;
}

.alpha-table-card-footer .ant-pagination {
    margin: 0;
}

@media (max-width: 767px) {
    .alpha-table-card-toolbar {
        justify-content: flex-start;
        padding-inline: 12px;
    }

    .alpha-table-card-footer {
        justify-content: flex-start;
        overflow-x: auto;
        padding: 14px 12px 16px;
    }

    .alpha-table-card-body {
        padding-inline: 0;
    }
}
</style>
