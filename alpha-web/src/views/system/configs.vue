<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    PushpinOutlined,
    ReloadOutlined,
    SettingOutlined,
} from '@ant-design/icons-vue'
import { message, Modal, type FormInstance } from 'ant-design-vue'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import VxeUITable from 'vxe-table'
import 'vxe-table/lib/style.css'
import type { VxeTableDefines } from 'vxe-table'

import TableActionMenu from '@/components/TableActionMenu.vue'
import {
    configApi,
    type Config,
    type ConfigDataType,
    type ConfigDefinition,
    type ConfigDefinitionSave,
} from '@/service/system'
import { configPageFromTableChange } from './configs.pagination'
import {
    validateConfigValue,
    validateDefinitionRules,
} from './configs.validation'

const { VxeColumn, VxeTable } = VxeUITable

const CONFIG_TABLE_SETTINGS_KEY = 'alpha-vue:table:system-configs:v1'
const CONFIG_TABLE_SETTINGS_VERSION = 1

const rows = ref<Config[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const definitions = ref<ConfigDefinition[]>([])
const definitionsLoading = ref(false)
const editorOpen = ref(false)
const definitionDrawerOpen = ref(false)
const definitionEditorOpen = ref(false)
const editingId = ref<number>()
const editingDefinitionId = ref<number>()
const selectedRowKeys = ref<number[]>([])
const tableRef = ref<{
    hideColumn?: (field: string) => Promise<void>
    showColumn?: (field: string) => Promise<void>
    setColumnFixed?: (field: string, fixed: 'left' | 'right') => Promise<void>
    clearColumnFixed?: (field: string) => Promise<void>
    setCustomStoreData?: (
        data: VxeTableDefines.CustomStoreData,
    ) => Promise<void>
}>()
const formRef = ref<FormInstance>()
const definitionFormRef = ref<FormInstance>()
const columnSettingsOpen = ref(false)
type ConfigColumnSetting = {
    field: string
    title: string
    visible: boolean
    fixed?: 'left' | 'right'
}
const columnSettings = ref<ConfigColumnSetting[]>([
    { field: 'configName', title: '参数名称', visible: true },
    { field: 'configKey', title: '配置键', visible: true },
    { field: 'dataType', title: '类型', visible: true },
    { field: 'enabled', title: '状态', visible: true },
    { field: 'configValue', title: '配置值', visible: true },
    { field: 'updatedAt', title: '更新时间', visible: true },
    { field: 'operate', title: '操作', visible: true, fixed: 'right' },
])

const emptyForm = () => ({ configKey: '', configValue: '', enabled: true })
const form = reactive(emptyForm())
const emptyDefinitionForm = () => ({
    configKey: 'file.',
    configName: '',
    valueType: 'STRING' as ConfigDataType,
    defaultValue: '',
    integerMin: undefined as number | undefined,
    integerMax: undefined as number | undefined,
    stringMaxLength: undefined as number | undefined,
    stringPattern: '',
    enumValues: '',
    sensitive: false,
    dynamic: false,
    runtimeBinding: undefined as string | undefined,
    status: 'DRAFT' as ConfigDefinition['status'],
})
const definitionForm = reactive(emptyDefinitionForm())
const runtimeBindings = [
    { value: 'FILE_UPLOAD_MAX_SIZE', label: '文件上传大小' },
    { value: 'FILE_UPLOAD_ALLOWED_EXTENSIONS', label: '文件上传扩展名' },
    { value: 'FILE_PRIVATE_ACCESS_TTL', label: '私有访问期限' },
]
const publishedDefinitions = computed(() =>
    definitions.value.filter((definition) => definition.status === 'PUBLISHED'),
)
const selectedDefinition = computed(() =>
    publishedDefinitions.value.find(
        (definition) => definition.configKey === form.configKey,
    ),
)
const configRules = {
    configKey: [{ required: true, message: '请选择已发布的配置项' }],
    configValue: [
        { required: true, whitespace: true, message: '请输入配置值' },
        { max: 10000, message: '配置值不能超过 10000 个字符' },
        {
            validator: (_rule: unknown, value: string) => {
                const error = validateConfigValue(
                    selectedDefinition.value,
                    value || '',
                )
                return error
                    ? Promise.reject(new Error(error))
                    : Promise.resolve()
            },
        },
    ],
}
const definitionRules = {
    configKey: [
        { required: true, message: '请输入配置键' },
        {
            pattern: /^file\.[A-Za-z][A-Za-z0-9._-]*$/,
            message: '配置键必须以 file. 开头',
        },
    ],
    configName: [
        { required: true, whitespace: true, message: '请输入配置名称' },
    ],
    defaultValue: [
        { required: true, whitespace: true, message: '请输入默认值' },
    ],
}
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) return rows.value
    return rows.value.filter(
        (item) =>
            item.configKey.toLowerCase().includes(value) ||
            item.configName.toLowerCase().includes(value) ||
            item.configGroup.toLowerCase().includes(value),
    )
})
const isFiltering = computed(() => Boolean(keyword.value.trim()))
const visibleTotal = computed(() =>
    isFiltering.value ? filteredRows.value.length : total.value,
)
const configTableHeight = computed(() =>
    Math.min(Math.max(filteredRows.value.length * 48 + 48, 160), 440),
)
function syncSelection(records: Config[]) {
    selectedRowKeys.value = records.map((record) => record.id)
}
function handleCheckboxChange(event: { records: Config[] }) {
    syncSelection(event.records)
}
function persistColumnSettings() {
    try {
        window.localStorage.setItem(
            CONFIG_TABLE_SETTINGS_KEY,
            JSON.stringify({
                version: CONFIG_TABLE_SETTINGS_VERSION,
                columns: columnSettings.value,
            }),
        )
    } catch {
        // Local storage can be unavailable in private browsing or test runtimes.
    }
}
function restoreColumnSettings() {
    try {
        const raw = window.localStorage.getItem(CONFIG_TABLE_SETTINGS_KEY)
        if (!raw) return
        const parsed: unknown = JSON.parse(raw)
        if (
            !parsed ||
            typeof parsed !== 'object' ||
            !('version' in parsed) ||
            parsed.version !== CONFIG_TABLE_SETTINGS_VERSION ||
            !('columns' in parsed) ||
            !Array.isArray(parsed.columns)
        ) {
            return
        }
        const saved = parsed.columns as ConfigColumnSetting[]
        columnSettings.value = columnSettings.value.map(
            (column) =>
                saved.find((item) => item.field === column.field) || column,
        )
        const savedOrder = saved.map((item) => item.field)
        columnSettings.value.sort(
            (left, right) =>
                savedOrder.indexOf(left.field) -
                savedOrder.indexOf(right.field),
        )
    } catch {
        // Invalid persisted settings fall back to the default column order.
    }
}
async function applyColumnSettings() {
    const table = tableRef.value
    if (!table) return
    await Promise.all(
        columnSettings.value.map((column) =>
            column.visible
                ? table.showColumn?.(column.field)
                : table.hideColumn?.(column.field),
        ),
    )
    await Promise.all(
        columnSettings.value.map((column) =>
            column.fixed
                ? table.setColumnFixed?.(column.field, column.fixed)
                : table.clearColumnFixed?.(column.field),
        ),
    )
    await table.setCustomStoreData?.({
        sortData: columnSettings.value.map((column) => ({ k: column.field })),
    })
    persistColumnSettings()
}
function toggleColumn(column: ConfigColumnSetting, visible: boolean) {
    const visibleCount = columnSettings.value.filter(
        (item) => item.visible,
    ).length
    if (!visible && visibleCount <= 1) return
    column.visible = visible
    void applyColumnSettings()
}
function toggleAllColumns(checked: boolean) {
    columnSettings.value.forEach((column) => {
        column.visible = checked
    })
    if (!checked) columnSettings.value[0].visible = true
    void applyColumnSettings()
}
function cycleColumnFixed(column: ConfigColumnSetting) {
    column.fixed =
        column.fixed === undefined
            ? 'left'
            : column.fixed === 'left'
              ? 'right'
              : undefined
    void applyColumnSettings()
}
function moveColumn(field: string, targetField: string) {
    const from = columnSettings.value.findIndex((item) => item.field === field)
    const to = columnSettings.value.findIndex(
        (item) => item.field === targetField,
    )
    if (from < 0 || to < 0 || from === to) return
    const [column] = columnSettings.value.splice(from, 1)
    columnSettings.value.splice(to, 0, column)
    void applyColumnSettings()
}
function handleAllColumnChange(event: { target: { checked: boolean } }) {
    toggleAllColumns(event.target.checked)
}
function handleColumnChange(
    column: ConfigColumnSetting,
    event: { target: { checked: boolean } },
) {
    toggleColumn(column, event.target.checked)
}
type ColumnDragEvent = {
    dataTransfer?: {
        setData: (format: string, data: string) => void
        getData: (format: string) => string
    } | null
}
function handleColumnDragStart(field: string, event: ColumnDragEvent) {
    event.dataTransfer?.setData('text/plain', field)
}
function handleColumnDrop(targetField: string, event: ColumnDragEvent) {
    moveColumn(event.dataTransfer?.getData('text/plain') || '', targetField)
}

async function load() {
    loading.value = true
    try {
        const response = await configApi.page(page.value, pageSize.value)
        rows.value = response.data.data.records
        total.value = response.data.data.total
        selectedRowKeys.value = []
    } finally {
        loading.value = false
    }
}
async function loadDefinitions() {
    definitionsLoading.value = true
    try {
        const response = await configApi.definitions()
        definitions.value = response.data.data.records
    } finally {
        definitionsLoading.value = false
    }
}
function openCreate() {
    editingId.value = undefined
    Object.assign(form, emptyForm())
    editorOpen.value = true
}
function openEdit(row: Config) {
    editingId.value = row.id
    Object.assign(form, {
        configKey: row.configKey,
        configValue: row.configValue || '',
        enabled: row.enabled,
    })
    editorOpen.value = true
}
async function submitForm() {
    try {
        await formRef.value?.validate()
    } catch {
        return
    }
    const payload = {
        configKey: form.configKey.trim(),
        configValue: form.configValue,
        enabled: form.enabled,
    }
    if (editingId.value) await configApi.update(editingId.value, payload)
    else await configApi.create(payload)
    message.success('保存成功')
    editorOpen.value = false
    await load()
}
function remove(row: Config) {
    Modal.confirm({
        title: `删除参数配置“${row.configKey}”？`,
        content: '删除后会移除其运行时缓存。',
        okType: 'danger',
        async onOk() {
            await configApi.delete(row.id)
            message.success('删除成功')
            await load()
        },
    })
}
function removeSelected() {
    const count = selectedRowKeys.value.length
    if (!count) return
    Modal.confirm({
        title: `删除已选的 ${count} 个参数配置？`,
        content: '删除后会移除对应的运行时缓存。',
        okText: '确认删除',
        cancelText: '取消',
        okType: 'danger',
        async onOk() {
            await Promise.all(
                selectedRowKeys.value.map((id) => configApi.delete(id)),
            )
            message.success(`已删除 ${count} 个参数配置`)
            await load()
        },
    })
}
function openDefinitionCreate() {
    editingDefinitionId.value = undefined
    Object.assign(definitionForm, emptyDefinitionForm())
    definitionEditorOpen.value = true
}
function openDefinitionEdit(definition: ConfigDefinition) {
    editingDefinitionId.value = definition.id
    Object.assign(definitionForm, {
        ...emptyDefinitionForm(),
        ...definition,
        defaultValue: definition.sensitive ? '' : definition.defaultValue || '',
        enumValues: definition.enumValues.join(','),
        stringPattern: definition.stringPattern || '',
    })
    definitionEditorOpen.value = true
}
function definitionPayload(): ConfigDefinitionSave {
    return {
        configKey: definitionForm.configKey.trim(),
        configName: definitionForm.configName.trim(),
        valueType: definitionForm.valueType,
        defaultValue: definitionForm.defaultValue,
        integerMin: definitionForm.integerMin,
        integerMax: definitionForm.integerMax,
        stringMaxLength: definitionForm.stringMaxLength,
        stringPattern: definitionForm.stringPattern.trim() || undefined,
        enumValues: definitionForm.enumValues.trim() || undefined,
        sensitive: definitionForm.sensitive,
        dynamic: definitionForm.dynamic,
        runtimeBinding: definitionForm.runtimeBinding,
        status: definitionForm.status,
    }
}
async function submitDefinition() {
    try {
        if (!definitionForm.sensitive || !editingDefinitionId.value) {
            await definitionFormRef.value?.validate()
        }
    } catch {
        return
    }
    const payload = definitionPayload()
    const error =
        payload.defaultValue || !editingDefinitionId.value
            ? validateDefinitionRules(payload)
            : undefined
    if (error) {
        message.error(error)
        return
    }
    if (editingDefinitionId.value) {
        await configApi.updateDefinition(editingDefinitionId.value, payload)
    } else {
        await configApi.createDefinition(payload)
    }
    message.success('配置定义已保存')
    definitionEditorOpen.value = false
    await Promise.all([loadDefinitions(), load()])
}
function changePage(pagination: { current?: number; pageSize?: number }) {
    const next = configPageFromTableChange(
        pagination,
        page.value,
        pageSize.value,
    )
    page.value = next.page
    pageSize.value = next.pageSize
    void load()
}
onMounted(() => {
    restoreColumnSettings()
    void Promise.all([load(), loadDefinitions()]).then(() =>
        nextTick(applyColumnSettings),
    )
})
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>参数配置</h1>
                <p>仅管理已发布的业务配置；动态配置由固定运行时绑定生效。</p>
            </div>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索名称、配置键或分组"
            />
            <a-button @click="keyword = ''">重置</a-button>
        </div>
        <section class="config-table-workspace">
            <div class="workspace-toolbar">
                <div>
                    <h2>参数列表</h2>
                    <p>
                        {{
                            isFiltering
                                ? `找到 ${visibleTotal} 条匹配结果`
                                : `共 ${total} 条业务参数`
                        }}
                    </p>
                </div>
                <a-space :size="8" wrap>
                    <a-button @click="load"
                        ><ReloadOutlined />刷新数据</a-button
                    >
                    <a-button
                        v-permission="'system:config:define'"
                        @click="definitionDrawerOpen = true"
                        ><SettingOutlined />配置定义</a-button
                    >
                    <a-button
                        v-permission="'system:config:create'"
                        type="primary"
                        @click="openCreate"
                        ><PlusOutlined />新增参数</a-button
                    >
                    <a-button
                        v-permission="'system:config:delete'"
                        danger
                        ghost
                        :disabled="!selectedRowKeys.length"
                        @click="removeSelected"
                        ><DeleteOutlined />批量删除</a-button
                    >
                    <a-popover
                        v-model:open="columnSettingsOpen"
                        placement="bottomRight"
                        trigger="click"
                    >
                        <template #content>
                            <div class="column-setting-panel">
                                <a-checkbox
                                    :checked="
                                        columnSettings.every(
                                            (column) => column.visible,
                                        )
                                    "
                                    :indeterminate="
                                        columnSettings.some(
                                            (column) => column.visible,
                                        ) &&
                                        !columnSettings.every(
                                            (column) => column.visible,
                                        )
                                    "
                                    @change="handleAllColumnChange"
                                    >全选</a-checkbox
                                >
                                <a-divider />
                                <div
                                    v-for="column in columnSettings"
                                    :key="column.field"
                                    class="column-setting-item"
                                    draggable="true"
                                    @dragover.prevent
                                    @drop="
                                        handleColumnDrop(column.field, $event)
                                    "
                                    @dragstart="
                                        handleColumnDragStart(
                                            column.field,
                                            $event,
                                        )
                                    "
                                >
                                    <span class="column-drag-handle">⋮⋮</span>
                                    <a-checkbox
                                        :checked="column.visible"
                                        @change="
                                            handleColumnChange(column, $event)
                                        "
                                        >{{ column.title }}</a-checkbox
                                    >
                                    <a-button
                                        type="text"
                                        size="small"
                                        :disabled="!column.visible"
                                        :title="
                                            column.fixed === 'left'
                                                ? '取消固定或固定到右侧'
                                                : column.fixed === 'right'
                                                  ? '取消固定'
                                                  : '固定到左侧'
                                        "
                                        @click="cycleColumnFixed(column)"
                                        ><PushpinOutlined
                                            :class="{
                                                'column-pin-active':
                                                    column.fixed,
                                            }"
                                    /></a-button>
                                </div>
                            </div>
                        </template>
                        <a-button type="primary" ghost>
                            <SettingOutlined />列设置
                        </a-button>
                    </a-popover>
                </a-space>
            </div>
            <div class="config-table-frame">
                <a-spin :spinning="loading">
                    <VxeTable
                        id="system-configs-table"
                        ref="tableRef"
                        class="configs-vxe-table"
                        :border="false"
                        auto-resize
                        size="small"
                        align="center"
                        header-align="center"
                        :row-config="{ keyField: 'id' }"
                        :data="filteredRows"
                        :height="configTableHeight"
                        :scroll-x="{ enabled: true }"
                        :scroll-y="{ enabled: true }"
                        :custom-config="{ enabled: true }"
                        :checkbox-config="{ checkRowKeys: selectedRowKeys }"
                        @checkbox-change="handleCheckboxChange"
                        @checkbox-all="handleCheckboxChange"
                    >
                        <VxeColumn type="checkbox" width="52" fixed="left" />
                        <VxeColumn
                            field="configName"
                            title="参数名称"
                            width="200"
                            show-overflow
                        />
                        <VxeColumn
                            field="configKey"
                            title="配置键"
                            width="250"
                            show-overflow
                        >
                            <template #default="{ row }">
                                <a-typography-text code>{{
                                    row.configKey
                                }}</a-typography-text>
                            </template>
                        </VxeColumn>
                        <VxeColumn field="dataType" title="类型" width="100">
                            <template #default="{ row }"
                                ><a-tag>{{ row.dataType }}</a-tag></template
                            >
                        </VxeColumn>
                        <VxeColumn
                            field="enabled"
                            title="状态"
                            width="100"
                            align="center"
                        >
                            <template #default="{ row }">
                                <a-badge
                                    :status="
                                        row.enabled ? 'success' : 'default'
                                    "
                                    :text="row.enabled ? '启用' : '禁用'"
                                />
                            </template>
                        </VxeColumn>
                        <VxeColumn
                            field="configValue"
                            title="配置值"
                            width="240"
                            show-overflow
                        >
                            <template #default="{ row }">
                                <span v-if="row.sensitive">已隐藏</span
                                ><span v-else>{{ row.configValue }}</span>
                            </template>
                        </VxeColumn>
                        <VxeColumn
                            field="updatedAt"
                            title="更新时间"
                            width="180"
                            show-overflow
                        />
                        <VxeColumn
                            field="operate"
                            title="操作"
                            width="100"
                            fixed="right"
                            align="center"
                        >
                            <template #default="{ row }">
                                <TableActionMenu aria-label="参数操作">
                                    <a-menu-item
                                        key="edit"
                                        v-permission="'system:config:update'"
                                        @click="openEdit(row)"
                                        ><EditOutlined />编辑</a-menu-item
                                    >
                                    <a-menu-item
                                        key="delete"
                                        v-permission="'system:config:delete'"
                                        danger
                                        @click="remove(row)"
                                        ><DeleteOutlined />删除</a-menu-item
                                    >
                                </TableActionMenu>
                            </template>
                        </VxeColumn>
                    </VxeTable>
                </a-spin>
                <a-pagination
                    class="config-table-pagination"
                    :current="page"
                    :page-size="pageSize"
                    :total="visibleTotal"
                    show-size-changer
                    @change="
                        (current: number, size: number) =>
                            changePage({ current, pageSize: size })
                    "
                />
            </div>
        </section>

        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑参数配置' : '新增参数配置'"
            ok-text="保存"
            cancel-text="取消"
            @ok="submitForm"
        >
            <a-form
                ref="formRef"
                :model="form"
                :rules="configRules"
                layout="vertical"
            >
                <a-form-item label="配置键" name="configKey" required
                    ><a-select
                        v-model:value="form.configKey"
                        :disabled="Boolean(editingId)"
                        placeholder="选择已发布配置项"
                        ><a-select-option
                            v-for="definition in publishedDefinitions"
                            :key="definition.configKey"
                            :value="definition.configKey"
                            >{{ definition.configName }}（{{
                                definition.configKey
                            }}）</a-select-option
                        ></a-select
                    ></a-form-item
                >
                <a-form-item label="配置值" name="configValue" required
                    ><a-textarea
                        v-model:value="form.configValue"
                        :rows="4"
                        :placeholder="
                            selectedDefinition?.sensitive
                                ? '敏感值不会回显；填写新值才会更新'
                                : undefined
                        "
                /></a-form-item>
                <a-alert
                    v-if="selectedDefinition?.sensitive"
                    type="warning"
                    show-icon
                    message="敏感配置值不会回显，也不会写入操作日志。"
                />
                <a-form-item label="启用状态" name="enabled"
                    ><a-switch
                        v-model:checked="form.enabled"
                        checked-children="启用"
                        un-checked-children="禁用"
                /></a-form-item>
            </a-form>
        </a-modal>

        <a-drawer
            v-model:open="definitionDrawerOpen"
            title="配置定义目录"
            width="min(760px, 100vw)"
        >
            <div class="definition-toolbar">
                <span>仅 `file.*` 业务配置可定义；敏感定义仅允许数据存储。</span
                ><a-button
                    v-permission="'system:config:define'"
                    type="primary"
                    @click="openDefinitionCreate"
                    ><PlusOutlined />新增定义</a-button
                >
            </div>
            <a-table
                row-key="id"
                :data-source="definitions"
                :loading="definitionsLoading"
                :scroll="{ x: 980 }"
                :pagination="false"
            >
                <a-table-column title="名称 / 键" width="250"
                    ><template #default="{ record }"
                        ><div>{{ record.configName }}</div>
                        <a-typography-text code>{{
                            record.configKey
                        }}</a-typography-text></template
                    ></a-table-column
                >
                <a-table-column
                    title="类型"
                    data-index="valueType"
                    width="90"
                />
                <a-table-column title="状态" data-index="status" width="100"
                    ><template #default="{ text }"
                        ><a-tag>{{ text }}</a-tag></template
                    ></a-table-column
                >
                <a-table-column title="边界" width="180"
                    ><template #default="{ record }"
                        ><span v-if="record.valueType === 'INTEGER'"
                            >{{ record.integerMin ?? '不限' }} -
                            {{ record.integerMax ?? '不限' }}</span
                        ><span v-else-if="record.valueType === 'ENUM'">{{
                            record.enumValues.join('，')
                        }}</span
                        ><span v-else>{{
                            record.stringMaxLength
                                ? `最多 ${record.stringMaxLength} 字符`
                                : '—'
                        }}</span></template
                    ></a-table-column
                >
                <a-table-column title="操作" width="76" fixed="right"
                    ><template #default="{ record }"
                        ><a-button
                            v-permission="'system:config:define'"
                            type="link"
                            @click="openDefinitionEdit(record)"
                            >编辑</a-button
                        ></template
                    ></a-table-column
                >
            </a-table>
        </a-drawer>

        <a-modal
            v-model:open="definitionEditorOpen"
            :title="editingDefinitionId ? '编辑配置定义' : '新增配置定义'"
            ok-text="保存"
            cancel-text="取消"
            :width="680"
            @ok="submitDefinition"
        >
            <a-form
                ref="definitionFormRef"
                :model="definitionForm"
                :rules="definitionRules"
                layout="vertical"
            >
                <div class="form-grid">
                    <a-form-item label="配置键" name="configKey"
                        ><a-input
                            v-model:value="definitionForm.configKey"
                            :disabled="
                                Boolean(editingDefinitionId)
                            " /></a-form-item
                    ><a-form-item label="配置名称" name="configName"
                        ><a-input v-model:value="definitionForm.configName"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="值类型"
                        ><a-select
                            v-model:value="definitionForm.valueType"
                            :disabled="Boolean(editingDefinitionId)"
                            ><a-select-option value="BOOLEAN"
                                >布尔</a-select-option
                            ><a-select-option value="INTEGER"
                                >整数</a-select-option
                            ><a-select-option value="ENUM">枚举</a-select-option
                            ><a-select-option value="STRING"
                                >文本</a-select-option
                            ></a-select
                        ></a-form-item
                    ><a-form-item label="发布状态"
                        ><a-select
                            v-model:value="definitionForm.status"
                            :disabled="definitionForm.dynamic"
                            ><a-select-option value="DRAFT"
                                >草稿</a-select-option
                            ><a-select-option value="PUBLISHED"
                                >已发布</a-select-option
                            ><a-select-option value="DISABLED"
                                >已禁用</a-select-option
                            ></a-select
                        ></a-form-item
                    >
                </div>
                <a-form-item
                    v-if="!definitionForm.sensitive || !editingDefinitionId"
                    label="默认值"
                    name="defaultValue"
                    ><a-textarea
                        v-model:value="definitionForm.defaultValue"
                        :rows="2"
                /></a-form-item>
                <a-alert
                    v-else
                    type="info"
                    show-icon
                    message="敏感默认值不会回显；留空将保留现有默认值。"
                />
                <div
                    v-if="definitionForm.valueType === 'INTEGER'"
                    class="form-grid"
                >
                    <a-form-item label="最小值"
                        ><a-input-number
                            v-model:value="definitionForm.integerMin"
                            class="full-width"
                            :precision="0" /></a-form-item
                    ><a-form-item label="最大值"
                        ><a-input-number
                            v-model:value="definitionForm.integerMax"
                            class="full-width"
                            :precision="0"
                    /></a-form-item>
                </div>
                <a-form-item
                    v-if="definitionForm.valueType === 'ENUM'"
                    label="枚举值"
                    ><a-input
                        v-model:value="definitionForm.enumValues"
                        placeholder="使用逗号分隔，例如 low,medium,high"
                /></a-form-item>
                <div
                    v-if="definitionForm.valueType === 'STRING'"
                    class="form-grid"
                >
                    <a-form-item label="最大长度"
                        ><a-input-number
                            v-model:value="definitionForm.stringMaxLength"
                            class="full-width"
                            :min="1"
                            :precision="0" /></a-form-item
                    ><a-form-item label="正则规则"
                        ><a-input v-model:value="definitionForm.stringPattern"
                    /></a-form-item>
                </div>
                <a-space wrap
                    ><a-switch
                        v-model:checked="definitionForm.sensitive"
                        :disabled="Boolean(editingDefinitionId)"
                        checked-children="敏感"
                        un-checked-children="非敏感" /><a-switch
                        v-model:checked="definitionForm.dynamic"
                        :disabled="Boolean(editingDefinitionId)"
                        checked-children="动态"
                        un-checked-children="数据存储"
                /></a-space>
                <a-form-item v-if="definitionForm.dynamic" label="运行时绑定"
                    ><a-select
                        v-model:value="definitionForm.runtimeBinding"
                        :disabled="Boolean(editingDefinitionId)"
                        ><a-select-option
                            v-for="binding in runtimeBindings"
                            :key="binding.value"
                            :value="binding.value"
                            >{{ binding.label }}</a-select-option
                        ></a-select
                    ></a-form-item
                >
                <a-alert
                    type="info"
                    show-icon
                    message="动态定义只能使用既有运行时绑定且必须保持发布；新增定义仅可作为数据存储。"
                />
            </a-form>
        </a-modal>
    </section>
</template>

<style scoped>
.config-table-workspace {
    margin-bottom: 16px;
    overflow: hidden;
    background: var(--alpha-surface);
    border-radius: var(--alpha-radius);
    box-shadow: 0 1px 2px rgb(0 0 0 / 10%);
}
.workspace-toolbar,
.definition-toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
}
.workspace-toolbar {
    padding-bottom: 12px;
}
.workspace-toolbar :deep(.ant-btn) {
    min-width: 92px;
    border-radius: 6px;
}
.workspace-toolbar :deep(.ant-btn-primary.ant-btn-background-ghost) {
    color: var(--alpha-primary);
    border-color: var(--alpha-primary);
}
.workspace-toolbar h2,
.workspace-toolbar p {
    margin: 0;
}
.workspace-toolbar p,
.definition-toolbar {
    color: var(--alpha-muted);
    font-size: 13px;
}
.selection-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 48px;
    padding: 0 16px;
    color: var(--alpha-primary-strong);
    background: var(--alpha-primary-soft);
    border-bottom: 1px solid var(--alpha-border-soft);
}
.definition-toolbar {
    padding: 0 0 16px;
}
.form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 16px;
}
.full-width {
    width: 100%;
}
.config-table-pagination {
    display: flex;
    justify-content: flex-end;
    padding: 14px 16px 16px;
}
.config-table-frame {
    margin: 0 16px 16px;
}
.config-table-frame :deep(.ant-spin-nested-loading) {
    overflow: hidden;
    background: var(--alpha-surface);
    border: 1px solid #eef1f5;
    border-radius: var(--alpha-radius);
}
.config-table-workspace :deep(.vxe-table) {
    width: 100%;
    color: var(--alpha-text);
    border: 0;
    --vxe-ui-table-header-background-color: #f8fafc;
    --vxe-ui-table-border-color: #eef1f5;
    --vxe-ui-table-fixed-scrolling-box-shadow-color: transparent;
    --vxe-ui-table-row-hover-background-color: #f5f8ff;
    --vxe-ui-table-row-height-small: 44px;
    --vxe-ui-table-cell-padding-small: 10px;
}
.config-table-workspace :deep(.configs-vxe-table),
.config-table-workspace :deep(.vxe-table--render-wrapper),
.config-table-workspace :deep(.vxe-table--main-wrapper),
.config-table-workspace :deep(.vxe-table--header-wrapper),
.config-table-workspace :deep(.vxe-table--body-wrapper) {
    width: 100%;
}
.config-table-workspace :deep(.vxe-table--render-wrapper) {
    border: 0;
    border-radius: inherit;
    overflow: hidden;
}
.config-table-workspace :deep(.vxe-table--header-wrapper) {
    background: #f8fafc;
    font-weight: 600;
}
.config-table-workspace :deep(.vxe-header--column) {
    color: #344054;
    font-size: 13px;
}
.config-table-workspace :deep(.vxe-body--column) {
    color: #475467;
    font-size: 13px;
}
.config-table-workspace :deep(.vxe-body--row.row--hover) {
    background: #f5f8ff;
}
.config-table-workspace :deep(.vxe-body--row) {
    transition: background-color 0.15s ease;
}
.config-table-workspace :deep(.vxe-header--column) {
    border-bottom: 1px solid #eef1f5;
}
.config-table-workspace :deep(.vxe-body--column) {
    border-bottom: 0.5px solid #eef1f5;
}
.config-table-workspace :deep(.vxe-body--row:last-child .vxe-body--column) {
    border-bottom: 0;
}
.config-table-workspace :deep(.vxe-cell) {
    line-height: 22px;
}
.config-table-workspace :deep(.vxe-table--border-line) {
    display: none;
}
.config-table-workspace :deep(.vxe-table--scroll-x-handle-appearance) {
    border-top: 0 !important;
}
.config-table-workspace :deep(.vxe-table--fixed-right-wrapper) {
    box-shadow: none !important;
}
.config-table-frame .config-table-pagination {
    padding: 14px 0 0;
}
.column-setting-panel {
    width: 280px;
    padding: 4px;
}
.column-setting-panel :deep(.ant-divider) {
    margin: 8px 0;
}
.column-setting-item {
    display: flex;
    gap: 8px;
    align-items: center;
    min-height: 36px;
    padding: 0 4px;
    border-radius: 4px;
}
.column-setting-item:hover {
    background: var(--alpha-primary-soft);
}
.column-setting-item :deep(.ant-checkbox-wrapper) {
    flex: 1;
    min-width: 0;
}
.column-drag-handle {
    color: var(--alpha-muted);
    cursor: grab;
    letter-spacing: -2px;
    user-select: none;
}
.column-pin-active {
    color: var(--alpha-primary);
}
@media (max-width: 767px) {
    .workspace-toolbar,
    .definition-toolbar,
    .selection-toolbar {
        align-items: flex-start;
        flex-direction: column;
    }
    .form-grid {
        grid-template-columns: 1fr;
    }
    .column-setting-panel {
        width: min(280px, calc(100vw - 48px));
    }
    .config-table-frame {
        margin: 0 10px 10px;
    }
}
</style>
