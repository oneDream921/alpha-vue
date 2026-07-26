<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import {
    dictApi,
    type DictItem,
    type DictItemSave,
    type DictType,
    type DictTypeSave,
} from '@/service/system'
import {
    dictionaryTypeRow,
    dictPageFromTableChange,
    itemPageForTypeSelection,
} from './dicts.pagination'
import { validateItemValue, validateTypeCode } from './dicts.validation'

const typeRows = ref<DictType[]>([])
const typeTotal = ref(0)
const typePage = ref(1)
const typePageSize = ref(10)
const typeLoading = ref(false)
const typeKeyword = ref('')
const selectedType = ref<DictType>()
const itemRows = ref<DictItem[]>([])
const itemTotal = ref(0)
const itemPage = ref(1)
const itemPageSize = ref(10)
const itemLoading = ref(false)
const typeEditorOpen = ref(false)
const itemEditorOpen = ref(false)
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

const typeRules = {
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
const itemRules = {
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
    if (next.shouldLoad) await loadItems()
}
function typeRow(type: DictType) {
    return dictionaryTypeRow(type, (selectedType) => {
        void selectType(selectedType)
    })
}
function changeTypePage(pagination: { current?: number; pageSize?: number }) {
    const next = dictPageFromTableChange(
        pagination,
        typePage.value,
        typePageSize.value,
    )
    typePage.value = next.page
    typePageSize.value = next.pageSize
    void loadTypes()
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

onMounted(loadTypes)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>数据字典</h1>
                <p>维护业务字段的统一可选值</p>
            </div>
            <a-button @click="loadTypes"><ReloadOutlined />刷新</a-button>
        </div>
        <a-row :gutter="16"
            ><a-col :xs="24" :lg="10"
                ><a-card title="字典类型">
                    <template #extra
                        ><a-button
                            v-permission="'system:dict:create'"
                            type="primary"
                            size="small"
                            @click="openTypeCreate"
                            ><PlusOutlined />新增</a-button
                        ></template
                    >
                    <div class="query-bar">
                        <a-input-search
                            v-model:value="typeKeyword"
                            allow-clear
                            placeholder="搜索类型编码或名称"
                        /><a-button @click="typeKeyword = ''">重置</a-button>
                    </div>
                    <a-table
                        row-key="id"
                        :data-source="filteredTypeRows"
                        :loading="typeLoading"
                        :pagination="{
                            current: typePage,
                            pageSize: typePageSize,
                            total: typeTotal,
                            showSizeChanger: true,
                        }"
                        :custom-row="typeRow"
                        @change="changeTypePage"
                    >
                        <a-table-column
                            title="类型编码"
                            data-index="typeCode"
                        /><a-table-column
                            title="类型名称"
                            data-index="typeName"
                        />
                        <a-table-column title="操作" width="130"
                            ><template #default="{ record }"
                                ><a-space
                                    ><a-button
                                        v-permission="'system:dict:update'"
                                        type="link"
                                        size="small"
                                        @click.stop="openTypeEdit(record)"
                                        ><EditOutlined />编辑</a-button
                                    ><a-button
                                        v-permission="'system:dict:delete'"
                                        type="link"
                                        danger
                                        size="small"
                                        @click.stop="removeType(record)"
                                        ><DeleteOutlined />删除</a-button
                                    ></a-space
                                ></template
                            ></a-table-column
                        >
                    </a-table>
                </a-card></a-col
            ><a-col :xs="24" :lg="14"
                ><a-card
                    :title="
                        selectedType
                            ? `${selectedType.typeName} 的字典项`
                            : '字典项'
                    "
                >
                    <template #extra
                        ><a-button
                            v-permission="'system:dict:create'"
                            type="primary"
                            size="small"
                            :disabled="!selectedType"
                            @click="openItemCreate"
                            ><PlusOutlined />新增字典项</a-button
                        ></template
                    >
                    <a-empty
                        v-if="!selectedType"
                        description="请选择左侧字典类型"
                    /><a-table
                        v-else
                        row-key="id"
                        :data-source="itemRows"
                        :loading="itemLoading"
                        :pagination="{
                            current: itemPage,
                            pageSize: itemPageSize,
                            total: itemTotal,
                            showSizeChanger: true,
                        }"
                        @change="changeItemPage"
                    >
                        <a-table-column
                            title="标签"
                            data-index="label"
                        /><a-table-column
                            title="值"
                            data-index="value"
                        /><a-table-column
                            title="排序"
                            data-index="sortOrder"
                            width="80"
                        /><a-table-column title="操作" width="130"
                            ><template #default="{ record }"
                                ><a-space
                                    ><a-button
                                        v-permission="'system:dict:update'"
                                        type="link"
                                        size="small"
                                        @click="openItemEdit(record)"
                                        ><EditOutlined />编辑</a-button
                                    ><a-button
                                        v-permission="'system:dict:delete'"
                                        type="link"
                                        danger
                                        size="small"
                                        @click="removeItem(record)"
                                        ><DeleteOutlined />删除</a-button
                                    ></a-space
                                ></template
                            ></a-table-column
                        >
                    </a-table>
                </a-card></a-col
            ></a-row
        >
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
