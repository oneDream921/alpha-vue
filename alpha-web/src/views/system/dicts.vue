<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
    SyncOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { computed, onMounted, reactive, ref } from 'vue'

import AlphaTableCard from '@/components/AlphaTableCard.vue'
import {
    dictApi,
    type DictItem,
    type DictItemSave,
    type DictType,
    type DictTypeSave,
} from '@/service/system/index'
import { dictStore } from '@/stores/dict'
import { parseDateTime } from '@/utils/dateTime'
import {
    dictPageFromTableChange,
    itemPageForTypeSelection,
} from './dicts.pagination'
import { validateItemValue, validateTypeCode } from './dicts.validation'

const typeRows = ref<DictType[]>([])
const typeTotal = ref(0)
const typePage = ref(1)
const typePageSize = ref(100)
const typeLoading = ref(false)
const typeKeyword = ref('')
const selectedType = ref<DictType>()
const itemRows = ref<DictItem[]>([])
const itemTotal = ref(0)
const itemPage = ref(1)
const itemPageSize = ref(10)
const itemLoading = ref(false)
const itemKeyword = ref('')
const selectedItemKeys = ref<(string | number)[]>([])
const typeEditorOpen = ref(false)
const itemEditorOpen = ref(false)
const cacheRefreshing = ref(false)
const editingTypeId = ref<number>()
const editingItemId = ref<number>()
const typeFormRef = ref()
const itemFormRef = ref()

const emptyTypeForm = (): DictTypeSave => ({
    typeCode: '',
    typeName: '',
    status: 1,
    remark: '',
})
const emptyItemForm = (): DictItemSave => ({
    label: '',
    value: '',
    sortOrder: 0,
    status: 1,
    isDefault: 0,
    remark: '',
})
const typeForm = reactive<DictTypeSave>(emptyTypeForm())
const itemForm = reactive<DictItemSave>(emptyItemForm())

const typeRules: Record<string, Rule[]> = {
    typeCode: [
        {
            validator: (_: unknown, value: string) => {
                const error = validateTypeCode(value || '')
                return error
                    ? Promise.reject(new Error(error))
                    : Promise.resolve()
            },
        },
    ],
    typeName: [
        { required: true, whitespace: true, message: '请输入类型名称' },
        { max: 64, message: '类型名称不能超过 64 个字符' },
    ],
    remark: [{ max: 500, message: '备注不能超过 500 个字符' }],
}
const itemRules: Record<string, Rule[]> = {
    label: [
        { required: true, whitespace: true, message: '请输入字典项标签' },
        { max: 64, message: '字典项标签不能超过 64 个字符' },
    ],
    value: [
        {
            validator: (_: unknown, value: string) => {
                const error = validateItemValue(value || '')
                return error
                    ? Promise.reject(new Error(error))
                    : Promise.resolve()
            },
        },
    ],
    sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
    remark: [{ max: 500, message: '备注不能超过 500 个字符' }],
}
const filteredTypeRows = computed(() => {
    const keyword = typeKeyword.value.trim().toLowerCase()
    return keyword
        ? typeRows.value.filter((type) =>
              `${type.typeCode} ${type.typeName} ${type.remark ?? ''}`
                  .toLowerCase()
                  .includes(keyword),
          )
        : typeRows.value
})
const typeTreeData = computed(() =>
    filteredTypeRows.value.map((type) => ({ key: type.id, title: type })),
)
const filteredItemRows = computed(() => {
    const keyword = itemKeyword.value.trim().toLowerCase()
    return keyword
        ? itemRows.value.filter((item) =>
              `${item.label} ${item.value} ${item.remark ?? ''}`
                  .toLowerCase()
                  .includes(keyword),
          )
        : itemRows.value
})

async function loadTypes() {
    typeLoading.value = true
    try {
        const response = await dictApi.pageTypes(
            typePage.value,
            typePageSize.value,
        )
        typeRows.value = response.data.data.records
        typeTotal.value = response.data.data.total
    } finally {
        typeLoading.value = false
    }
}
async function refreshDictCache() {
    cacheRefreshing.value = true
    try {
        const response = await dictApi.refreshCache()
        dictStore.clear()
        message.success(`已刷新 ${response.data.data.typeCount} 个字典类型缓存`)
    } finally {
        cacheRefreshing.value = false
    }
}
async function loadItems() {
    if (!selectedType.value) return
    itemLoading.value = true
    try {
        const response = await dictApi.pageItems(
            selectedType.value.id,
            itemPage.value,
            itemPageSize.value,
        )
        itemRows.value = response.data.data.records
        itemTotal.value = response.data.data.total
    } finally {
        itemLoading.value = false
    }
}
async function selectType(type: DictType) {
    const next = itemPageForTypeSelection(type.id, itemPageSize.value)
    selectedType.value = type
    itemPage.value = next.page
    itemPageSize.value = next.pageSize
    itemRows.value = []
    itemTotal.value = 0
    selectedItemKeys.value = []
    if (next.shouldLoad) await loadItems()
}
function changeTypePage(nextPage: number, nextPageSize: number) {
    typePage.value = nextPage
    typePageSize.value = nextPageSize
    void loadTypes()
}
function handleTypeTreeSelect(selectedKeys: (string | number)[]) {
    const selectedId = Number(selectedKeys[0])
    const type = typeRows.value.find((item) => item.id === selectedId)
    if (type) void selectType(type)
}
function changeItemPage(pagination: { current?: number; pageSize?: number }) {
    const next = dictPageFromTableChange(
        pagination,
        itemPage.value,
        itemPageSize.value,
    )
    itemPage.value = next.page
    itemPageSize.value = next.pageSize
    void loadItems()
}
type DictItemTableColumn = {
    key: string
    dataIndex?: string
    title: string
    width?: number
    minWidth?: number
    align?: 'left' | 'center' | 'right'
}
const itemTableColumns: DictItemTableColumn[] = [
    { key: 'label', dataIndex: 'label', title: '字典标签', minWidth: 160 },
    { key: 'value', dataIndex: 'value', title: '字典键值', minWidth: 200 },
    {
        key: 'sortOrder',
        dataIndex: 'sortOrder',
        title: '字典排序',
        width: 80,
        align: 'center',
    },
    { key: 'remark', dataIndex: 'remark', title: '备注', minWidth: 180 },
    {
        key: 'createdAt',
        dataIndex: 'createdAt',
        title: '创建时间',
        minWidth: 180,
    },
    { key: 'operate', title: '操作', width: 108, align: 'center' },
]
const itemRowSelection = computed(() => ({
    selectedRowKeys: selectedItemKeys.value,
    onChange: (keys: (string | number)[]) => {
        selectedItemKeys.value = keys
    },
}))
function formatTime(value?: string) {
    if (!value) return '-'
    const date = parseDateTime(value)
    if (Number.isNaN(date.getTime())) return '-'
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
        date.getDate(),
    )} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
        date.getSeconds(),
    )}`
}
function openTypeCreate() {
    editingTypeId.value = undefined
    Object.assign(typeForm, emptyTypeForm())
    typeEditorOpen.value = true
}
function openTypeEdit(type: DictType) {
    editingTypeId.value = type.id
    Object.assign(typeForm, {
        typeCode: type.typeCode,
        typeName: type.typeName,
        status: type.status,
        remark: type.remark ?? '',
    })
    typeEditorOpen.value = true
}
async function saveType() {
    try {
        await typeFormRef.value?.validate()
    } catch {
        return
    }
    const payload = {
        ...typeForm,
        typeCode: typeForm.typeCode.trim(),
        typeName: typeForm.typeName.trim(),
        remark: typeForm.remark?.trim() || undefined,
    }
    if (editingTypeId.value)
        await dictApi.updateType(editingTypeId.value, payload)
    else await dictApi.createType(payload)
    message.success('保存成功')
    typeEditorOpen.value = false
    await loadTypes()
}
function removeType(type: DictType) {
    Modal.confirm({
        title: `删除字典类型“${type.typeName}”？`,
        content: '请先删除该字典类型下的字典项。',
        okType: 'danger',
        async onOk() {
            await dictApi.deleteType(type.id)
            if (selectedType.value?.id === type.id) {
                selectedType.value = undefined
                itemRows.value = []
                itemTotal.value = 0
            }
            message.success('删除成功')
            await loadTypes()
        },
    })
}
function openItemCreate() {
    if (!selectedType.value) return
    editingItemId.value = undefined
    Object.assign(itemForm, emptyItemForm())
    itemEditorOpen.value = true
}
function openItemEdit(item: DictItem) {
    editingItemId.value = item.id
    Object.assign(itemForm, {
        label: item.label,
        value: item.value,
        sortOrder: item.sortOrder,
        status: item.status,
        isDefault: item.isDefault,
        remark: item.remark ?? '',
    })
    itemEditorOpen.value = true
}
function openItemEditRecord(record: unknown) {
    openItemEdit(record as DictItem)
}
async function saveItem() {
    if (!selectedType.value) return
    try {
        await itemFormRef.value?.validate()
    } catch {
        return
    }
    const payload = {
        ...itemForm,
        label: itemForm.label.trim(),
        value: itemForm.value.trim(),
        remark: itemForm.remark?.trim() || undefined,
    }
    if (editingItemId.value)
        await dictApi.updateItem(editingItemId.value, payload)
    else await dictApi.createItem(selectedType.value.id, payload)
    message.success('保存成功')
    itemEditorOpen.value = false
    await loadItems()
}
function removeItem(item: DictItem) {
    Modal.confirm({
        title: `删除字典项“${item.label}”？`,
        okType: 'danger',
        async onOk() {
            await dictApi.deleteItem(item.id)
            message.success('删除成功')
            await loadItems()
        },
    })
}
function removeItemRecord(record: unknown) {
    removeItem(record as DictItem)
}
function removeSelectedItems() {
    const count = selectedItemKeys.value.length
    if (!count) return
    Modal.confirm({
        title: `删除选中的 ${count} 个字典项？`,
        content: '删除后无法恢复，请确认选择无误。',
        okType: 'danger',
        async onOk() {
            await Promise.all(
                selectedItemKeys.value.map((id) =>
                    dictApi.deleteItem(Number(id)),
                ),
            )
            selectedItemKeys.value = []
            message.success('删除成功')
            await loadItems()
        },
    })
}

onMounted(loadTypes)
</script>

<template>
    <section class="page-section dict-page">
        <a-row :gutter="20" class="dict-layout">
            <a-col :xs="24" :lg="8" :xl="7">
                <aside class="dict-type-panel">
                    <header class="dict-panel-header">
                        <h1>字典类型列表</h1>
                        <a-space :size="4">
                            <a-button
                                v-permission="'system:dict:create'"
                                type="text"
                                aria-label="新增字典类型"
                                title="新增字典类型"
                                @click="openTypeCreate"
                                ><PlusOutlined
                            /></a-button>
                            <a-button
                                type="text"
                                aria-label="刷新字典类型"
                                title="刷新字典类型"
                                @click="loadTypes"
                                ><ReloadOutlined
                            /></a-button>
                        </a-space>
                    </header>
                    <a-input
                        v-model:value="typeKeyword"
                        allow-clear
                        class="dict-type-search"
                        placeholder="请输入关键词搜索"
                    />
                    <a-spin :spinning="typeLoading">
                        <a-tree
                            class="dict-type-tree"
                            block-node
                            show-line
                            :tree-data="typeTreeData"
                            :selected-keys="
                                selectedType ? [selectedType.id] : []
                            "
                            @select="handleTypeTreeSelect"
                        >
                            <template #title="{ title: type }">
                                <div
                                    class="dict-type-tree-node"
                                    @click.stop="selectType(type)"
                                >
                                    <a-tooltip
                                        :title="`${type.typeName}（${type.typeCode}）`"
                                    >
                                        <span class="dict-type-list-item"
                                            ><strong>{{ type.typeName }}</strong
                                            ><span
                                                >（{{ type.typeCode }}）</span
                                            ></span
                                        >
                                    </a-tooltip>
                                    <div class="dict-type-actions">
                                        <a-button
                                            v-permission="'system:dict:update'"
                                            type="text"
                                            size="small"
                                            aria-label="编辑字典类型"
                                            title="编辑字典类型"
                                            @click.stop="openTypeEdit(type)"
                                            ><EditOutlined
                                        /></a-button>
                                        <a-button
                                            v-permission="'system:dict:delete'"
                                            type="text"
                                            danger
                                            size="small"
                                            aria-label="删除字典类型"
                                            title="删除字典类型"
                                            @click.stop="removeType(type)"
                                            ><DeleteOutlined
                                        /></a-button>
                                    </div>
                                </div>
                            </template>
                        </a-tree>
                        <a-empty
                            v-if="!typeLoading && typeTreeData.length === 0"
                            description="暂无字典类型"
                            :image-style="{ height: '72px' }"
                        />
                    </a-spin>
                    <a-pagination
                        v-if="typeTotal > typePageSize"
                        class="dict-type-pagination"
                        size="small"
                        :current="typePage"
                        :page-size="typePageSize"
                        :total="typeTotal"
                        :show-size-changer="true"
                        @change="changeTypePage"
                    />
                </aside>
            </a-col>
            <a-col :xs="24" :lg="16" :xl="17">
                <div class="dict-content-area">
                    <a-collapse ghost class="dict-search-panel">
                        <a-collapse-panel key="search" header="搜索">
                            <div class="dict-search-form">
                                <a-input
                                    v-model:value="itemKeyword"
                                    allow-clear
                                    placeholder="搜索字典标签、键值或备注"
                                />
                                <a-button @click="itemKeyword = ''"
                                    >重置</a-button
                                >
                            </div>
                        </a-collapse-panel>
                    </a-collapse>
                    <section class="dict-data-panel">
                        <header class="dict-data-toolbar">
                            <h2>
                                {{
                                    selectedType
                                        ? `${selectedType.typeName}（${selectedType.typeCode}）`
                                        : '字典列表'
                                }}
                            </h2>
                            <div class="dict-toolbar-actions">
                                <a-button
                                    v-permission="'system:dict:update'"
                                    :loading="cacheRefreshing"
                                    @click="refreshDictCache"
                                    ><SyncOutlined />刷新缓存</a-button
                                >
                                <a-button
                                    v-permission="'system:dict:create'"
                                    type="primary"
                                    :disabled="!selectedType"
                                    @click="openItemCreate"
                                    ><PlusOutlined />新增</a-button
                                >
                                <a-button
                                    v-permission="'system:dict:delete'"
                                    danger
                                    :disabled="selectedItemKeys.length === 0"
                                    @click="removeSelectedItems"
                                    ><DeleteOutlined />批量删除</a-button
                                >
                                <a-button
                                    @click="
                                        selectedType ? loadItems() : loadTypes()
                                    "
                                    ><ReloadOutlined />刷新</a-button
                                >
                            </div>
                        </header>
                        <a-empty
                            v-if="!selectedType"
                            class="dict-item-empty"
                            description="请从左侧选择一个字典类型"
                        />
                        <AlphaTableCard v-else :loading="itemLoading">
                            <a-table
                                row-key="id"
                                :data-source="filteredItemRows"
                                :columns="itemTableColumns"
                                :row-selection="itemRowSelection"
                                :pagination="false"
                                :scroll="{ x: 'max-content' }"
                            >
                                <template #bodyCell="{ column, record }">
                                    <template v-if="column.key === 'createdAt'">
                                        {{ formatTime(record.createdAt) }}
                                    </template>
                                    <template
                                        v-else-if="column.key === 'operate'"
                                    >
                                        <a-space :size="8">
                                            <a-button
                                                v-permission="
                                                    'system:dict:update'
                                                "
                                                type="text"
                                                size="small"
                                                aria-label="编辑字典项"
                                                title="编辑字典项"
                                                @click="
                                                    openItemEditRecord(record)
                                                "
                                                ><EditOutlined
                                            /></a-button>
                                            <a-button
                                                v-permission="
                                                    'system:dict:delete'
                                                "
                                                type="text"
                                                danger
                                                size="small"
                                                aria-label="删除字典项"
                                                title="删除字典项"
                                                @click="
                                                    removeItemRecord(record)
                                                "
                                                ><DeleteOutlined
                                            /></a-button>
                                        </a-space>
                                    </template>
                                </template>
                            </a-table>
                            <template #footer>
                                <a-pagination
                                    :current="itemPage"
                                    :page-size="itemPageSize"
                                    :total="itemTotal"
                                    show-size-changer
                                    :show-total="(count) => `共 ${count} 条`"
                                    @change="
                                        (current, pageSize) =>
                                            changeItemPage({
                                                current,
                                                pageSize,
                                            })
                                    "
                                />
                            </template>
                        </AlphaTableCard>
                    </section>
                </div>
            </a-col>
        </a-row>
        <a-modal
            v-model:open="typeEditorOpen"
            :title="editingTypeId ? '编辑字典类型' : '新增字典类型'"
            ok-text="保存"
            @ok="saveType"
            ><a-form
                ref="typeFormRef"
                :model="typeForm"
                :rules="typeRules"
                layout="vertical"
                ><a-form-item label="类型编码" name="typeCode"
                    ><a-input
                        v-model:value="typeForm.typeCode"
                        :disabled="Boolean(editingTypeId)" /></a-form-item
                ><a-form-item label="类型名称" name="typeName" required
                    ><a-input v-model:value="typeForm.typeName" /></a-form-item
                ><a-form-item label="状态"
                    ><a-select v-model:value="typeForm.status"
                        ><a-select-option :value="1">启用</a-select-option
                        ><a-select-option :value="0"
                            >停用</a-select-option
                        ></a-select
                    ></a-form-item
                ><a-form-item label="备注" name="remark"
                    ><a-textarea
                        v-model:value="typeForm.remark"
                        :rows="2" /></a-form-item></a-form
        ></a-modal>
        <a-modal
            v-model:open="itemEditorOpen"
            :title="editingItemId ? '编辑字典项' : '新增字典项'"
            ok-text="保存"
            @ok="saveItem"
            ><a-form
                ref="itemFormRef"
                :model="itemForm"
                :rules="itemRules"
                layout="vertical"
                ><a-form-item label="标签" name="label" required
                    ><a-input v-model:value="itemForm.label" /></a-form-item
                ><a-form-item label="值" name="value" required
                    ><a-input v-model:value="itemForm.value" /></a-form-item
                ><a-form-item label="排序" name="sortOrder"
                    ><a-input-number
                        v-model:value="itemForm.sortOrder"
                        :min="0"
                        class="full-width" /></a-form-item
                ><a-form-item label="状态"
                    ><a-select v-model:value="itemForm.status"
                        ><a-select-option :value="1">启用</a-select-option
                        ><a-select-option :value="0"
                            >停用</a-select-option
                        ></a-select
                    ></a-form-item
                ><a-form-item label="默认项"
                    ><a-select v-model:value="itemForm.isDefault"
                        ><a-select-option :value="1">是</a-select-option
                        ><a-select-option :value="0"
                            >否</a-select-option
                        ></a-select
                    ></a-form-item
                ><a-form-item label="备注" name="remark"
                    ><a-textarea
                        v-model:value="itemForm.remark"
                        :rows="2" /></a-form-item></a-form
        ></a-modal>
    </section>
</template>

<style scoped>
.dict-page {
    padding-top: 20px;
}

.dict-type-panel,
.dict-data-panel,
.dict-search-panel {
    overflow: hidden;
    background: var(--alpha-surface);
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
}

.dict-type-panel {
    min-height: 640px;
    padding: 18px 16px;
}

.dict-panel-header,
.dict-data-toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
}

.dict-panel-header {
    min-height: 32px;
    margin-bottom: 18px;
}

.dict-panel-header h1,
.dict-data-toolbar h2 {
    margin: 0;
    color: var(--alpha-text);
    font-size: 18px;
    font-weight: 650;
    white-space: nowrap;
}

.dict-type-search {
    margin-bottom: 14px;
}

.dict-type-tree {
    min-height: 420px;
}

.dict-type-tree :deep(.ant-tree-treenode) {
    width: 100%;
    padding-block: 3px;
}

.dict-type-tree :deep(.ant-tree-node-content-wrapper) {
    width: calc(100% - 24px);
    height: auto;
    min-height: 42px;
    padding: 0 8px;
    border-radius: 4px;
    transition: background-color 0.16s ease;
}

.dict-type-tree :deep(.ant-tree-node-content-wrapper:hover) {
    background: var(--alpha-primary-soft);
}

.dict-type-tree :deep(.ant-tree-title) {
    display: block;
    width: 100%;
}

.dict-type-tree-node {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    width: 100%;
    gap: 8px;
    align-items: center;
    cursor: pointer;
}

.dict-type-tree-node:hover .dict-type-list-item {
    color: var(--alpha-primary-strong);
}

.dict-type-list-item {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    color: var(--alpha-text);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.dict-type-list-item strong,
.dict-type-list-item span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.dict-type-list-item strong {
    font-weight: 600;
}

.dict-type-list-item span {
    margin-left: 4px;
    color: var(--alpha-muted);
}

.dict-type-actions {
    display: flex;
    gap: 2px;
    align-items: center;
    justify-content: flex-end;
    min-width: 58px;
}

.dict-type-actions :deep(.ant-btn) {
    width: 28px;
    padding-inline: 0;
}

.dict-type-pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
}

.dict-content-area {
    display: grid;
    gap: 16px;
}

.dict-search-panel :deep(.ant-collapse-header) {
    min-height: 54px;
    align-items: center;
    color: var(--alpha-text);
    font-weight: 600;
}

.dict-search-form {
    display: flex;
    gap: 12px;
    padding: 0 16px 16px;
}

.dict-search-form :deep(.ant-input-affix-wrapper),
.dict-search-form :deep(.ant-input) {
    flex: 1;
}

.dict-data-toolbar {
    display: block;
    padding: 16px 18px;
    border-bottom: 1px solid var(--alpha-border-soft);
}

.dict-data-toolbar h2 {
    margin-bottom: 14px;
}

.dict-toolbar-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    min-width: 0;
    max-width: 100%;
    flex-wrap: wrap;
}

.dict-data-panel :deep(.ant-table-wrapper) {
    padding: 14px 18px 18px;
}

.dict-data-panel :deep(.ant-table) {
    border: 1px solid var(--alpha-border-soft);
    border-radius: 8px;
    overflow: hidden;
}

.dict-data-panel :deep(.ant-table-thead > tr > th) {
    color: var(--alpha-text);
    font-weight: 600;
}

.dict-data-panel :deep(.ant-table-tbody > tr > td),
.dict-data-panel :deep(.ant-table-thead > tr > th) {
    padding-top: 15px;
    padding-bottom: 15px;
}

.dict-item-empty {
    display: grid;
    min-height: 420px;
    place-content: center;
}

@media (max-width: 991px) {
    .dict-type-panel {
        min-height: auto;
        margin-bottom: 16px;
    }

    .dict-type-tree {
        min-height: 180px;
        max-height: 280px;
        overflow: auto;
    }

    .dict-data-toolbar {
        padding-block: 14px;
    }
}

@media (max-width: 767px) {
    .dict-page {
        padding-top: 12px;
    }

    .dict-toolbar-actions > :deep(.ant-btn) {
        flex: 1 1 auto;
    }

    .dict-type-actions {
        opacity: 1;
    }
}
</style>
