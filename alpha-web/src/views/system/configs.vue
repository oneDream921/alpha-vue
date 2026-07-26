<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import { configApi, type Config } from '@/service/system'
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
const formRef = ref()
const emptyForm = () => ({
    configKey: '',
    configValue: '',
    description: '',
})
const form = reactive(emptyForm())
const rules = {
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
    description: [{ max: 500, message: '说明不能超过 500 个字符' }],
}
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) return rows.value
    return rows.value.filter(
        (item) =>
            item.configKey.toLowerCase().includes(value) ||
            item.description?.toLowerCase().includes(value),
    )
})

async function load() {
    loading.value = true
    try {
        const response = await configApi.page(page.value, pageSize.value)
        rows.value = response.data.data.records
        total.value = response.data.data.total
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
        configValue: row.configValue,
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
        configKey: form.configKey.trim(),
        configValue: form.configValue,
        description: form.description.trim() || undefined,
    }
    if (editingId.value) await configApi.update(editingId.value, payload)
    else await configApi.create(payload)
    message.success('保存成功；参数不会自动生效')
    editorOpen.value = false
    await load()
}
function remove(row: Config) {
    Modal.confirm({
        title: `删除参数配置“${row.configKey}”？`,
        content: '删除不会自动影响当前运行中的基础设施配置。',
        okType: 'danger',
        async onOk() {
            await configApi.delete(row.id)
            message.success('删除成功')
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
                <p>仅维护业务参数；基础设施配置仍由环境变量管理</p>
            </div>
            <a-space wrap>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'system:config:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新增参数配置</a-button
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索配置键或说明"
            />
            <a-button @click="keyword = ''">重置</a-button>
        </div>
        <a-table
            row-key="id"
            :data-source="filteredRows"
            :loading="loading"
            :scroll="{ x: 900 }"
            :pagination="{
                current: page,
                pageSize,
                total,
                showSizeChanger: true,
                showTotal: (count: number) => `共 ${count} 条`,
            }"
            @change="changePage"
        >
            <a-table-column title="配置键" data-index="configKey" width="240" />
            <a-table-column title="配置值" data-index="configValue" ellipsis />
            <a-table-column title="说明" data-index="description" width="220" />
            <a-table-column title="操作" width="160" align="center"
                ><template #default="{ record }"
                    ><a-space
                        ><a-button
                            v-permission="'system:config:update'"
                            type="link"
                            size="small"
                            @click="openEdit(record)"
                            ><EditOutlined />编辑</a-button
                        ><a-button
                            v-permission="'system:config:delete'"
                            type="link"
                            danger
                            size="small"
                            @click="remove(record)"
                            ><DeleteOutlined />删除</a-button
                        ></a-space
                    ></template
                ></a-table-column
            >
        </a-table>
        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑参数配置' : '新增参数配置'"
            ok-text="保存"
            cancel-text="取消"
            @ok="submitForm"
            ><a-form
                ref="formRef"
                :model="form"
                :rules="rules"
                layout="vertical"
                ><a-form-item label="配置键" name="configKey" required
                    ><a-input
                        v-model:value="form.configKey"
                        autocomplete="off" /></a-form-item
                ><a-form-item label="配置值" name="configValue" required
                    ><a-textarea
                        v-model:value="form.configValue"
                        :rows="4" /></a-form-item
                ><a-form-item label="说明" name="description"
                    ><a-textarea
                        v-model:value="form.description"
                        :rows="2" /></a-form-item
                ><a-alert
                    type="info"
                    show-icon
                    message="保存后不会自动生效；密钥、密码、令牌及基础设施配置键不允许保存。" /></a-form
        ></a-modal>
    </section>
</template>
