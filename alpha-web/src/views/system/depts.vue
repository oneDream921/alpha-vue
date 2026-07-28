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

import TableActionMenu from '@/components/TableActionMenu.vue'
import { deptApi, type Dept } from '@/service/system'

const rows = ref<Dept[]>([])
const loading = ref(false)
const editorOpen = ref(false)
const editingId = ref<number>()
const formRef = ref()
const expandedRowKeys = ref<number[]>([])
const keyword = ref('')
const emptyForm = () => ({
    parentId: undefined as number | undefined,
    name: '',
    sortOrder: 0,
    status: 1,
})
const form = reactive(emptyForm())
const rules: Record<string, Rule[]> = {
    name: [
        { required: true, whitespace: true, message: '请输入部门名称' },
        { max: 64, message: '部门名称不能超过 64 个字符' },
    ],
    parentId: [{ type: 'number', min: 0, message: '请选择有效的上级部门' }],
    sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
}

type DeptTreeNode = { title: string; value: number; children?: DeptTreeNode[] }
type DeptRow = Dept & { children: DeptRow[] }

function descendantIds(parentId: number, ids = new Set<number>()) {
    ids.add(parentId)
    rows.value
        .filter((item) => item.parentId === parentId)
        .forEach((item) => descendantIds(item.id, ids))
    return ids
}

const parentDeptOptions = computed<DeptTreeNode[]>(() => {
    const excluded = editingId.value
        ? descendantIds(editingId.value)
        : new Set()
    const nodes = new Map<number, DeptTreeNode>()
    rows.value
        .filter((item) => !excluded.has(item.id))
        .forEach((item) =>
            nodes.set(item.id, {
                title: item.name,
                value: item.id,
                children: [],
            }),
        )
    const roots: DeptTreeNode[] = []
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
    const nodes = new Map<number, DeptRow>()
    rows.value.forEach((item) => nodes.set(item.id, { ...item, children: [] }))
    const roots: DeptRow[] = []
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
    const include = (node: DeptRow): DeptRow | null => {
        const children = node.children
            .map(include)
            .filter((item): item is DeptRow => item !== null)
        return node.name.toLowerCase().includes(value) || children.length
            ? { ...node, children }
            : null
    }
    return treeRows.value
        .map(include)
        .filter((item): item is DeptRow => item !== null)
})
async function load() {
    loading.value = true
    try {
        rows.value = (await deptApi.page(1, 100)).data.data.records
        expandedRowKeys.value = rows.value.map((item) => item.id)
    } finally {
        loading.value = false
    }
}
function updateExpanded(expanded: boolean, record: Dept) {
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
function openEdit(row: Dept) {
    editingId.value = row.id
    Object.assign(form, {
        parentId: Number(row.parentId) === 0 ? undefined : row.parentId,
        name: row.name,
        sortOrder: row.sortOrder,
        status: row.status,
    })
    editorOpen.value = true
}
function setParentId(value: string | number | null | undefined) {
    form.parentId = value == null ? undefined : Number(value)
}
async function save() {
    const payload = {
        parentId: form.parentId ?? 0,
        name: form.name.trim(),
        sortOrder: form.sortOrder ?? 0,
        status: form.status,
    }
    if (editingId.value) await deptApi.update(editingId.value, payload)
    else await deptApi.create(payload)
    message.success('保存成功')
    editorOpen.value = false
    await load()
}
async function submitForm() {
    await formRef.value?.validate()
    await save()
}
function remove(row: Dept) {
    Modal.confirm({
        title: `删除部门“${row.name}”？`,
        content: '存在子部门或关联用户时后端会拒绝删除。',
        okType: 'danger',
        async onOk() {
            await deptApi.delete(row.id)
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
                <h1>部门管理</h1>
                <p>维护组织结构和部门状态</p>
            </div>
            <a-space wrap>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'system:dept:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新建部门</a-button
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索部门名称"
            />
            <a-button @click="keyword = ''">重置</a-button>
        </div>
        <a-table
            row-key="id"
            :data-source="filteredTreeRows"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 680 }"
            :expanded-row-keys="expandedRowKeys"
            @expand="updateExpanded"
        >
            <a-table-column title="部门名称" data-index="name" width="240" />
            <a-table-column
                title="排序"
                data-index="sortOrder"
                width="100"
                align="center"
            />
            <a-table-column
                title="状态"
                data-index="status"
                width="100"
                align="center"
                ><template #default="{ text }"
                    ><a-badge
                        :status="text === 1 ? 'success' : 'default'"
                        :text="text === 1 ? '启用' : '停用'" /></template
            ></a-table-column>
            <a-table-column title="操作" width="88" align="center"
                ><template #default="{ record }"
                    ><TableActionMenu aria-label="部门操作"
                        ><a-menu-item
                            key="edit"
                            v-permission="'system:dept:update'"
                            @click="openEdit(record)"
                            ><EditOutlined />编辑</a-menu-item
                        ><a-menu-item
                            key="delete"
                            v-permission="'system:dept:delete'"
                            danger
                            @click="remove(record)"
                            ><DeleteOutlined />删除</a-menu-item
                        ></TableActionMenu
                    ></template
                ></a-table-column
            >
        </a-table>
        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑部门' : '新建部门'"
            ok-text="保存"
            cancel-text="取消"
            @ok="submitForm"
            ><a-form
                ref="formRef"
                :model="form"
                :rules="rules"
                layout="vertical"
            >
                <a-form-item label="部门名称" name="name" required
                    ><a-input v-model:value="form.name"
                /></a-form-item>
                <div class="form-grid">
                    <a-form-item label="上级部门" name="parentId"
                        ><a-tree-select
                            :value="form.parentId"
                            class="full-width"
                            :tree-data="parentDeptOptions"
                            placeholder="请选择上级部门（不选为顶级）"
                            :field-names="{
                                label: 'title',
                                value: 'value',
                                children: 'children',
                            }"
                            allow-clear
                            tree-default-expand-all
                            @update:value="setParentId" /></a-form-item
                    ><a-form-item label="排序" name="sortOrder"
                        ><a-input-number
                            v-model:value="form.sortOrder"
                            class="full-width"
                    /></a-form-item>
                </div>
                <a-form-item label="状态"
                    ><a-switch
                        v-model:checked="form.status"
                        :checked-value="1"
                        :un-checked-value="0"
                /></a-form-item> </a-form
        ></a-modal>
    </section>
</template>
