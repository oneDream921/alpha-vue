<script setup lang="ts">
import {
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { computed, onMounted, reactive, ref } from 'vue'

import { authApi } from '@/service/auth'
import { ensureManagementRoutes } from '@/router'
import { menuApi, type Menu } from '@/service/system'
import { authStore } from '@/stores/auth'

const rows = ref<Menu[]>([])
const loading = ref(false)
const editorOpen = ref(false)
const editingId = ref<number>()
const formRef = ref()
const expandedRowKeys = ref<number[]>([])
const keyword = ref('')
const emptyForm = () => ({
    parentId: undefined as number | undefined,
    title: '',
    menuType: 'MENU' as Menu['menuType'],
    path: '',
    component: '',
    permission: '',
    icon: '',
    sortOrder: 0,
    visible: 1,
    status: 1,
})
const form = reactive(emptyForm())
const rules: Record<string, Rule[]> = {
    title: [
        { required: true, whitespace: true, message: '请输入菜单名称' },
        { max: 64, message: '菜单名称不能超过 64 个字符' },
    ],
    menuType: [{ required: true, message: '请选择菜单类型' }],
    parentId: [{ type: 'number', min: 0, message: '请选择有效的上级菜单' }],
    sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
    path: [{ max: 128, message: '路由路径不能超过 128 个字符' }],
    component: [{ max: 255, message: '组件标识不能超过 255 个字符' }],
    permission: [{ max: 128, message: '权限编码不能超过 128 个字符' }],
    icon: [{ max: 64, message: '图标不能超过 64 个字符' }],
}

type MenuTreeNode = { title: string; value: number; children?: MenuTreeNode[] }
type MenuRow = Menu & { children: MenuRow[] }
function descendantIds(parentId: number, ids = new Set<number>()) {
    ids.add(parentId)
    rows.value
        .filter((item) => item.parentId === parentId)
        .forEach((item) => descendantIds(item.id, ids))
    return ids
}
const parentMenuOptions = computed<MenuTreeNode[]>(() => {
    const excluded = editingId.value
        ? descendantIds(editingId.value)
        : new Set()
    const nodes = new Map<number, MenuTreeNode>()
    rows.value
        .filter((item) => !excluded.has(item.id))
        .forEach((item) =>
            nodes.set(item.id, {
                title: item.title,
                value: item.id,
                children: [],
            }),
        )
    const roots: MenuTreeNode[] = []
    rows.value.forEach((item) => {
        const node = nodes.get(item.id)
        if (!node) return
        const parent = nodes.get(item.parentId ?? 0)
        if (parent) parent.children?.push(node)
        else roots.push(node)
    })
    return roots
})
const treeRows = computed(() => {
    const nodes = new Map<number, MenuRow>()
    rows.value.forEach((item) => nodes.set(item.id, { ...item, children: [] }))
    const roots: MenuRow[] = []
    nodes.forEach((node) => {
        const parent = nodes.get(node.parentId ?? 0)
        if (parent) parent.children.push(node)
        else roots.push(node)
    })
    return roots
})
const filteredTreeRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    if (!value) return treeRows.value
    const include = (node: MenuRow): MenuRow | null => {
        const children = node.children
            .map(include)
            .filter((item): item is MenuRow => item !== null)
        const text =
            `${node.title} ${node.path ?? ''} ${node.permission ?? ''}`.toLowerCase()
        return text.includes(value) || children.length
            ? { ...node, children }
            : null
    }
    return treeRows.value
        .map(include)
        .filter((item): item is MenuRow => item !== null)
})

async function load() {
    loading.value = true
    try {
        rows.value = (await menuApi.page(1, 100)).data.data.records
        expandedRowKeys.value = rows.value.map((item) => item.id)
    } finally {
        loading.value = false
    }
}
function updateExpanded(expanded: boolean, record: Menu) {
    const keys = new Set(expandedRowKeys.value)
    if (expanded) keys.add(record.id)
    else keys.delete(record.id)
    expandedRowKeys.value = [...keys]
}
function openCreate() {
    editingId.value = undefined
    Object.assign(form, emptyForm())
    editorOpen.value = true
}
function openCreateChild(row: Menu) {
    editingId.value = undefined
    Object.assign(form, { ...emptyForm(), parentId: row.id })
    editorOpen.value = true
}
function expandAll() {
    expandedRowKeys.value = rows.value.map((item) => item.id)
}
function collapseAll() {
    expandedRowKeys.value = []
}
function openEdit(row: Menu) {
    editingId.value = row.id
    Object.assign(form, {
        parentId: Number(row.parentId) === 0 ? undefined : row.parentId,
        title: row.title,
        menuType: row.menuType,
        path: row.path ?? '',
        component: row.component ?? '',
        permission: row.permission ?? '',
        icon: row.icon ?? '',
        sortOrder: row.sortOrder,
        visible: row.visible,
        status: row.status,
    })
    editorOpen.value = true
}
function payload() {
    return {
        ...form,
        parentId: form.parentId ?? 0,
        path: form.path || undefined,
        component: form.component || undefined,
        permission: form.permission || undefined,
        icon: form.icon || undefined,
    }
}
async function submitForm() {
    await formRef.value?.validate()
    await save()
}
async function save() {
    if (editingId.value) await menuApi.update(editingId.value, payload())
    else await menuApi.create(payload())
    const routes = (await authApi.routes()).data.data
    authStore.setRoutes(routes)
    ensureManagementRoutes(routes)
    message.success('保存成功')
    editorOpen.value = false
    await load()
}
function remove(row: Menu) {
    Modal.confirm({
        title: `删除菜单“${row.title}”？`,
        content: '存在子菜单时后端会拒绝删除。',
        okType: 'danger',
        async onOk() {
            await menuApi.delete(row.id)
            const routes = (await authApi.routes()).data.data
            authStore.setRoutes(routes)
            ensureManagementRoutes(routes)
            message.success('删除成功')
            await load()
        },
    })
}
onMounted(load)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>菜单管理</h1>
                <p>维护导航、页面和按钮权限编码</p>
            </div>
            <a-space wrap>
                <a-button @click="collapseAll">全部收起</a-button>
                <a-button @click="expandAll">全部展开</a-button>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'system:menu:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新建菜单</a-button
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索菜单名称、路由或权限编码"
            />
            <a-button @click="keyword = ''">重置</a-button>
        </div>
        <a-table
            row-key="id"
            :data-source="filteredTreeRows"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1050 }"
            :expanded-row-keys="expandedRowKeys"
            @expand="updateExpanded"
        >
            <a-table-column title="名称" data-index="title" width="160" />
            <a-table-column
                title="类型"
                data-index="menuType"
                width="110"
                align="center"
                ><template #default="{ text }"
                    ><a-tag>{{
                        { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮' }[
                            text as Menu['menuType']
                        ]
                    }}</a-tag></template
                ></a-table-column
            >
            <a-table-column title="路由" data-index="path" width="160"
                ><template #default="{ text }">{{
                    text || '-'
                }}</template></a-table-column
            >
            <a-table-column title="组件" data-index="component" width="180"
                ><template #default="{ text }">{{
                    text || '-'
                }}</template></a-table-column
            >
            <a-table-column title="权限编码" data-index="permission" width="220"
                ><template #default="{ text }"
                    ><a-typography-text v-if="text" code>{{
                        text
                    }}</a-typography-text
                    ><span v-else>-</span></template
                ></a-table-column
            >
            <a-table-column
                title="排序"
                data-index="sortOrder"
                width="80"
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
            <a-table-column title="操作" width="150" align="center"
                ><template #default="{ record }"
                    ><a-space :size="4">
                        <a-button
                            v-permission="'system:menu:create'"
                            type="text"
                            size="small"
                            title="新增子菜单"
                            @click.stop="openCreateChild(record)"
                            ><PlusOutlined
                        /></a-button>
                        <a-button
                            v-permission="'system:menu:update'"
                            type="text"
                            size="small"
                            title="编辑菜单"
                            @click.stop="openEdit(record)"
                            ><EditOutlined
                        /></a-button>
                        <a-button
                            v-permission="'system:menu:delete'"
                            type="text"
                            danger
                            size="small"
                            title="删除菜单"
                            @click.stop="remove(record)"
                            ><DeleteOutlined
                        /></a-button> </a-space></template
            ></a-table-column>
        </a-table>
        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑菜单' : '新建菜单'"
            width="680px"
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
                    <a-form-item label="名称" name="title" required
                        ><a-input v-model:value="form.title" /></a-form-item
                    ><a-form-item label="类型" name="menuType" required
                        ><a-segmented
                            v-model:value="form.menuType"
                            :options="[
                                { label: '目录', value: 'DIRECTORY' },
                                { label: '菜单', value: 'MENU' },
                                { label: '按钮', value: 'BUTTON' },
                            ]"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="上级菜单" name="parentId"
                        ><a-tree-select
                            v-model:value="form.parentId"
                            class="full-width"
                            :tree-data="parentMenuOptions"
                            :field-names="{
                                label: 'title',
                                value: 'value',
                                children: 'children',
                            }"
                            placeholder="不选则为顶级菜单"
                            allow-clear
                            tree-default-expand-all /></a-form-item
                    ><a-form-item label="排序" name="sortOrder"
                        ><a-input-number
                            v-model:value="form.sortOrder"
                            class="full-width"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="路由路径" name="path"
                        ><a-input v-model:value="form.path" /></a-form-item
                    ><a-form-item label="组件标识" name="component"
                        ><a-input v-model:value="form.component"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="权限编码" name="permission"
                        ><a-input
                            v-model:value="form.permission" /></a-form-item
                    ><a-form-item label="图标" name="icon"
                        ><a-input v-model:value="form.icon"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="导航可见"
                        ><a-switch
                            v-model:checked="form.visible"
                            :checked-value="1"
                            :un-checked-value="0" /></a-form-item
                    ><a-form-item label="状态"
                        ><a-switch
                            v-model:checked="form.status"
                            :checked-value="1"
                            :un-checked-value="0"
                    /></a-form-item>
                </div> </a-form
        ></a-modal>
    </section>
</template>
