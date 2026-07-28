<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal, type TableProps } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import TableActionMenu from '@/components/TableActionMenu.vue'
import { configApi, type Config, type ConfigDataType } from '@/service/system'
import { configPageFromTableChange } from './configs.pagination'
import { validateConfigKey } from './configs.validation'

const rows = ref<Config[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const editorOpen = ref(false)
const editingId = ref<number>()
const selectedRowKeys = ref<number[]>([])
const formRef = ref()
const emptyForm = () => ({
    configName: '',
    configKey: '',
    configValue: '',
    configGroup: 'general',
    dataType: 'STRING' as ConfigDataType,
    enabled: true,
    description: '',
})
const form = reactive(emptyForm())
const rules = {
    configName: [
        { required: true, whitespace: true, message: '请输入参数名称' },
        { max: 64, message: '参数名称不能超过 64 个字符' },
    ],
    configKey: [
        { required: true, whitespace: true, message: '请输入配置键' },
        { max: 128, message: '配置键不能超过 128 个字符' },
        {
            pattern: /^[A-Za-z][A-Za-z0-9._-]*$/,
            message: '配置键只能使用字母、数字、点、下划线或连字符',
        },
        {
            validator: (_rule: unknown, value: string) => {
                const error = validateConfigKey(value || '')
                return error
                    ? Promise.reject(new Error(error))
                    : Promise.resolve()
            },
        },
    ],
    configValue: [
        { required: true, whitespace: true, message: '请输入配置值' },
        { max: 10000, message: '配置值不能超过 10000 个字符' },
    ],
    configGroup: [
        { required: true, whitespace: true, message: '请输入参数分组' },
        { max: 64, message: '参数分组不能超过 64 个字符' },
    ],
    description: [{ max: 500, message: '说明不能超过 500 个字符' }],
}
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) return rows.value
    return rows.value.filter(
        (item) =>
            item.configKey.toLowerCase().includes(value) ||
            item.configName.toLowerCase().includes(value) ||
            item.configGroup.toLowerCase().includes(value) ||
            item.description?.toLowerCase().includes(value),
    )
})
const isFiltering = computed(() => Boolean(keyword.value.trim()))
const visibleTotal = computed(() =>
    isFiltering.value ? filteredRows.value.length : total.value,
)
const rowSelection = computed<TableProps['rowSelection']>(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys) => {
        selectedRowKeys.value = keys as number[]
    },
}))

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
function openCreate() {
    editingId.value = undefined
    Object.assign(form, emptyForm())
    editorOpen.value = true
}
function openEdit(row: Config) {
    editingId.value = row.id
    Object.assign(form, {
        configKey: row.configKey,
        configName: row.configName,
        configValue: row.configValue,
        configGroup: row.configGroup,
        dataType: row.dataType,
        enabled: row.enabled,
        description: row.description || '',
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
        configName: form.configName.trim(),
        configKey: form.configKey.trim(),
        configValue: form.configValue,
        configGroup: form.configGroup.trim(),
        dataType: form.dataType,
        enabled: form.enabled,
        description: form.description.trim() || undefined,
    }
    if (editingId.value) await configApi.update(editingId.value, payload)
    else await configApi.create(payload)
    message.success('保存成功，Redis 运行时参数已同步')
    editorOpen.value = false
    await load()
}
function remove(row: Config) {
    Modal.confirm({
        title: `删除参数配置“${row.configKey}”？`,
        content: '删除后会立即移除 Redis 运行时缓存，不会影响基础设施配置。',
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
        content:
            '删除后无法在参数管理中恢复，并会立即移除对应的 Redis 运行时缓存。',
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
onMounted(load)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>参数配置</h1>
                <p>集中维护业务参数，保存后将立即同步 Redis 运行时缓存</p>
            </div>
            <a-space wrap class="page-heading-actions">
                <a-button @click="load"><ReloadOutlined />刷新数据</a-button>
                <a-button
                    v-permission="'system:config:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新增参数</a-button
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索名称、配置键、分组或用途说明"
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
            </div>
            <div v-if="selectedRowKeys.length" class="selection-toolbar">
                <span>已选择 {{ selectedRowKeys.length }} 项</span>
                <a-space :size="8">
                    <a-button size="small" @click="selectedRowKeys = []"
                        >取消选择</a-button
                    >
                    <a-button
                        v-permission="'system:config:delete'"
                        danger
                        size="small"
                        @click="removeSelected"
                        ><DeleteOutlined />批量删除</a-button
                    >
                </a-space>
            </div>
            <a-table
                row-key="id"
                :data-source="filteredRows"
                :loading="loading"
                :row-selection="rowSelection"
                :scroll="{ x: 1400 }"
                :pagination="{
                    current: page,
                    pageSize,
                    total: visibleTotal,
                    showSizeChanger: true,
                    showTotal: (count: number) => `共 ${count} 条`,
                }"
                @change="changePage"
            >
                <a-table-column
                    title="参数名称"
                    data-index="configName"
                    width="160"
                    ellipsis
                />
                <a-table-column
                    title="配置键"
                    data-index="configKey"
                    width="220"
                >
                    <template #default="{ text }">
                        <a-typography-text code>{{ text }}</a-typography-text>
                    </template>
                </a-table-column>
                <a-table-column
                    title="分组"
                    data-index="configGroup"
                    width="120"
                >
                    <template #default="{ text }">
                        {{ text }}
                    </template>
                </a-table-column>
                <a-table-column title="类型" data-index="dataType" width="90">
                    <template #default="{ text }">
                        <a-tag>{{ text }}</a-tag>
                    </template>
                </a-table-column>
                <a-table-column
                    title="状态"
                    data-index="enabled"
                    width="90"
                    align="center"
                >
                    <template #default="{ text }">
                        <a-badge
                            :status="text ? 'success' : 'default'"
                            :text="text ? '启用' : '禁用'"
                        />
                    </template>
                </a-table-column>
                <a-table-column
                    title="配置值"
                    data-index="configValue"
                    width="200"
                    ellipsis
                />
                <a-table-column
                    title="更新时间"
                    data-index="updatedAt"
                    width="180"
                    ellipsis
                />
                <a-table-column
                    title="用途说明"
                    data-index="description"
                    width="220"
                    ellipsis
                >
                    <template #default="{ text }">
                        <span class="config-description">{{
                            text || '—'
                        }}</span>
                    </template>
                </a-table-column>
                <a-table-column
                    title="操作"
                    width="88"
                    fixed="right"
                    align="center"
                    ><template #default="{ record }"
                        ><TableActionMenu aria-label="参数操作">
                            <a-menu-item
                                key="edit"
                                v-permission="'system:config:update'"
                                @click="openEdit(record)"
                                ><EditOutlined />编辑</a-menu-item
                            >
                            <a-menu-item
                                key="delete"
                                v-permission="'system:config:delete'"
                                danger
                                @click="remove(record)"
                                ><DeleteOutlined />删除</a-menu-item
                            >
                        </TableActionMenu></template
                    ></a-table-column
                >
            </a-table>
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
                :rules="rules"
                layout="vertical"
            >
                <a-form-item label="参数名称" name="configName" required>
                    <a-input
                        v-model:value="form.configName"
                        autocomplete="off"
                    />
                </a-form-item>
                <a-form-item label="配置键" name="configKey" required>
                    <a-input
                        v-model:value="form.configKey"
                        autocomplete="off"
                    />
                </a-form-item>
                <a-form-item label="配置值" name="configValue" required>
                    <a-textarea v-model:value="form.configValue" :rows="4" />
                </a-form-item>
                <div class="config-form-grid">
                    <a-form-item label="参数分组" name="configGroup" required>
                        <a-input
                            v-model:value="form.configGroup"
                            autocomplete="off"
                        />
                    </a-form-item>
                    <a-form-item label="数据类型" name="dataType" required>
                        <a-select v-model:value="form.dataType">
                            <a-select-option value="STRING"
                                >文本</a-select-option
                            >
                            <a-select-option value="NUMBER"
                                >数字</a-select-option
                            >
                            <a-select-option value="BOOLEAN"
                                >布尔</a-select-option
                            >
                            <a-select-option value="JSON">JSON</a-select-option>
                        </a-select>
                    </a-form-item>
                </div>
                <a-form-item label="启用状态" name="enabled">
                    <a-switch
                        v-model:checked="form.enabled"
                        checked-children="启用"
                        un-checked-children="禁用"
                    />
                </a-form-item>
                <a-form-item label="说明" name="description">
                    <a-textarea v-model:value="form.description" :rows="2" />
                </a-form-item>
                <a-alert
                    type="info"
                    show-icon
                    message="保存成功后会立即同步到 Redis；禁用或删除后会同步移除。密钥、密码、令牌及基础设施配置键不允许保存。"
                />
            </a-form>
        </a-modal>
    </section>
</template>

<style scoped>
:deep(.ant-table-wrapper) {
    border: 0;
}

.config-table-workspace {
    margin-bottom: 16px;
    overflow: hidden;
    background: var(--alpha-surface);
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    box-shadow: var(--alpha-shadow);
}

.workspace-toolbar p {
    margin: 0;
    color: var(--alpha-muted);
    font-size: 13px;
    line-height: 20px;
}

.workspace-toolbar {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    min-height: 64px;
    padding: 0 16px;
    border-bottom: 1px solid var(--alpha-border-soft);
}

.workspace-toolbar h2 {
    margin: 0 0 2px;
    font-size: 16px;
}

.selection-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 48px;
    padding: 0 16px;
    color: var(--alpha-primary-strong);
    background: var(--alpha-primary-soft);
    border-bottom: 1px solid #dbeafe;
}

.config-description {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: middle;
    white-space: nowrap;
}

.config-form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 16px;
}

.config-description {
    color: var(--alpha-muted);
}

@media (max-width: 767px) {
    .workspace-toolbar {
        align-items: flex-start;
        flex-direction: column;
        padding-block: 14px;
    }

    .selection-toolbar {
        gap: 8px;
        align-items: flex-start;
        flex-direction: column;
        padding-block: 10px;
    }

    .config-form-grid {
        grid-template-columns: 1fr;
    }
}
</style>
