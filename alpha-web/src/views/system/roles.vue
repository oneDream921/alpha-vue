<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
    SafetyOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { computed, onMounted, reactive, ref } from 'vue'

import { menuApi, roleApi, type Menu, type Role } from '@/service/system'

const rows = ref<Role[]>([])
const menus = ref<Menu[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const editorOpen = ref(false)
const permissionOpen = ref(false)
const editingId = ref<number>()
const formRef = ref()
const assigningRole = ref<Role>()
const selectedMenuIds = ref<number[]>([])
const emptyForm = () => ({
    name: '',
    code: '',
    sortOrder: 0,
    status: 1,
    remark: '',
})
const form = reactive(emptyForm())
const rules: Record<string, Rule[]> = {
    name: [
        { required: true, whitespace: true, message: '请输入角色名称' },
        { max: 64, message: '角色名称不能超过 64 个字符' },
    ],
    code: [
        { required: true, whitespace: true, message: '请输入角色编码' },
        {
            pattern: /^[A-Z][A-Z0-9_]*$/,
            message: '角色编码需以大写字母开头，仅含大写字母、数字和下划线',
        },
        { max: 64, message: '角色编码不能超过 64 个字符' },
    ],
    sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
    remark: [{ max: 500, message: '备注不能超过 500 个字符' }],
}
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    return value
        ? rows.value.filter((item) =>
              `${item.name} ${item.code} ${item.remark ?? ''}`
                  .toLowerCase()
                  .includes(value),
          )
        : rows.value
})

async function load() {
    loading.value = true
    try {
        const response = await roleApi.page(page.value, size.value)
        rows.value = response.data.data.records
        total.value = response.data.data.total
    } finally {
        loading.value = false
    }
}
function changePage(pagination: { current?: number; pageSize?: number }) {
    page.value = pagination.current ?? 1
    size.value = pagination.pageSize ?? 10
    void load()
}
async function loadMenus() {
    menus.value = (await menuApi.page(1, 100)).data.data.records
}
function openCreate() {
    editingId.value = undefined
    Object.assign(form, emptyForm())
    editorOpen.value = true
}
function openEdit(row: Role) {
    editingId.value = row.id
    Object.assign(form, {
        name: row.name,
        code: row.code,
        sortOrder: row.sortOrder,
        status: row.status,
        remark: row.remark ?? '',
    })
    editorOpen.value = true
}
async function save() {
    if (editingId.value)
        await roleApi.update(editingId.value, {
            name: form.name,
            sortOrder: form.sortOrder,
            status: form.status,
            remark: form.remark || undefined,
        })
    else await roleApi.create({ ...form, remark: form.remark || undefined })
    message.success('保存成功')
    editorOpen.value = false
    await load()
}
async function submitForm() {
    await formRef.value?.validate()
    await save()
}
function remove(row: Role) {
    Modal.confirm({
        title: `删除角色“${row.name}”？`,
        okType: 'danger',
        async onOk() {
            await roleApi.delete(row.id)
            message.success('删除成功')
            await load()
        },
    })
}
async function openPermissions(row: Role) {
    assigningRole.value = row
    selectedMenuIds.value = (await roleApi.menuIds(row.id)).data.data
    permissionOpen.value = true
}
async function savePermissions() {
    if (!assigningRole.value) return
    await roleApi.assignMenus(assigningRole.value.id, selectedMenuIds.value)
    message.success('权限已更新')
    permissionOpen.value = false
}
onMounted(async () => {
    await Promise.all([load(), loadMenus()])
})
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>角色管理</h1>
                <p>配置角色状态和菜单按钮权限</p>
            </div>
            <a-space wrap>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'system:role:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新建角色</a-button
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索角色名称、编码或备注"
            />
            <a-button @click="keyword = ''">重置</a-button>
        </div>
        <a-table
            row-key="id"
            :data-source="filteredRows"
            :loading="loading"
            :pagination="{
                current: page,
                pageSize: size,
                total,
                showSizeChanger: true,
            }"
            :scroll="{ x: 800 }"
            @change="changePage"
        >
            <a-table-column title="角色名称" data-index="name" width="180" />
            <a-table-column title="编码" data-index="code" width="180"
                ><template #default="{ text }"
                    ><a-typography-text code>{{
                        text
                    }}</a-typography-text></template
                ></a-table-column
            >
            <a-table-column
                title="排序"
                data-index="sortOrder"
                width="90"
                align="center"
            />
            <a-table-column
                title="状态"
                data-index="status"
                width="90"
                align="center"
                ><template #default="{ text }"
                    ><a-badge
                        :status="text === 1 ? 'success' : 'default'"
                        :text="text === 1 ? '启用' : '停用'" /></template
            ></a-table-column>
            <a-table-column title="备注" data-index="remark" width="220"
                ><template #default="{ text }">{{
                    text || '-'
                }}</template></a-table-column
            >
            <a-table-column title="操作" width="168" align="center"
                ><template #default="{ record }"
                    ><a-space>
                        <a-button
                            v-permission="'system:role:update'"
                            type="link"
                            size="small"
                            @click="openEdit(record)"
                            ><EditOutlined />编辑</a-button
                        >
                        <a-button
                            v-permission="'system:role:assign'"
                            type="link"
                            size="small"
                            @click="openPermissions(record)"
                            ><SafetyOutlined />权限</a-button
                        >
                        <a-button
                            v-if="record.code !== 'SUPER_ADMIN'"
                            v-permission="'system:role:delete'"
                            type="link"
                            danger
                            size="small"
                            @click="remove(record)"
                            ><DeleteOutlined />删除</a-button
                        >
                    </a-space></template
                ></a-table-column
            >
        </a-table>
        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑角色' : '新建角色'"
            ok-text="保存"
            cancel-text="取消"
            @ok="submitForm"
            ><a-form
                ref="formRef"
                :model="form"
                :rules="rules"
                layout="vertical"
            >
                <div class="form-grid">
                    <a-form-item label="名称" name="name" required
                        ><a-input v-model:value="form.name" /></a-form-item
                    ><a-form-item label="编码" name="code" required
                        ><a-input
                            v-model:value="form.code"
                            :disabled="!!editingId"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="排序" name="sortOrder"
                        ><a-input-number
                            v-model:value="form.sortOrder"
                            class="full-width" /></a-form-item
                    ><a-form-item label="状态"
                        ><a-switch
                            v-model:checked="form.status"
                            :checked-value="1"
                            :un-checked-value="0"
                    /></a-form-item>
                </div>
                <a-form-item label="备注" name="remark"
                    ><a-textarea v-model:value="form.remark" :rows="3"
                /></a-form-item> </a-form
        ></a-modal>
        <a-modal
            v-model:open="permissionOpen"
            title="分配菜单与按钮权限"
            ok-text="保存"
            cancel-text="取消"
            @ok="savePermissions"
            ><a-checkbox-group
                v-model:value="selectedMenuIds"
                class="assignment-list"
                ><a-checkbox
                    v-for="item in menus"
                    :key="item.id"
                    :value="item.id"
                    >{{ item.title
                    }}<span v-if="item.permission" class="permission-code">{{
                        item.permission
                    }}</span></a-checkbox
                ></a-checkbox-group
            ></a-modal
        >
    </section>
</template>
