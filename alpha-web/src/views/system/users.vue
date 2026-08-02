<script setup lang="ts">
import {
    DeleteOutlined,
    DisconnectOutlined,
    EditOutlined,
    KeyOutlined,
    PlusOutlined,
    ReloadOutlined,
    SafetyOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import axios from 'axios'
import { computed, onMounted, reactive, ref } from 'vue'

import TableActionMenu from '@/components/TableActionMenu.vue'
import AlphaTableCard from '@/components/AlphaTableCard.vue'
import TableColumnSetting, {
    type TableColumnSettingItem,
} from '@/components/TableColumnSetting.vue'
import {
    deptApi,
    roleApi,
    userApi,
    type Dept,
    type Role,
    type User,
} from '@/service/system/index'
import { authStore } from '@/stores/auth'

const rows = ref<User[]>([])
const roles = ref<Role[]>([])
const depts = ref<Dept[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const selectedDeptId = ref<number>()
const editorOpen = ref(false)
const roleOpen = ref(false)
const resetPasswordOpen = ref(false)
const editingId = ref<number>()
const assigningUser = ref<User>()
const selectedRoleIds = ref<number[]>([])
const resettingUser = ref<User>()
const resetPasswordFormRef = ref()
const resetPasswordForm = reactive({ newPassword: '', confirmPassword: '' })
const formRef = ref()
const columnSettings = ref<TableColumnSettingItem[]>([
    { field: 'username', title: '账号', visible: true, align: 'left' },
    { field: 'nickname', title: '昵称', visible: true, align: 'left' },
    { field: 'email', title: '邮箱', visible: true, align: 'left' },
    { field: 'deptId', title: '部门', visible: true, align: 'left' },
    { field: 'status', title: '状态', visible: true, align: 'center' },
    { field: 'operate', title: '操作', visible: true, align: 'center' },
])
const tableSettingsKey = 'alpha-vue:table:system-users:v1'
const rules: Record<string, Rule[]> = {
    username: [
        { required: true, message: '请输入账号' },
        { max: 64, message: '账号不能超过 64 个字符' },
    ],
    password: [
        { required: true, message: '请输入初始密码' },
        { min: 8, message: '初始密码至少为 8 位' },
    ],
    nickname: [
        { required: true, message: '请输入昵称' },
        { max: 64, message: '昵称不能超过 64 个字符' },
    ],
    email: [{ type: 'email', message: '请输入正确的邮箱地址' }],
    phone: [
        { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入 11 位中国大陆手机号' },
    ],
}

const emptyForm = () => ({
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    deptId: undefined as number | undefined,
    status: 1,
})
const form = reactive(emptyForm())
type DeptTreeNode = { key: number; title: string; children: DeptTreeNode[] }
const deptTreeData = computed<DeptTreeNode[]>(() => {
    const nodes = new Map<number, DeptTreeNode>()
    depts.value.forEach((dept) =>
        nodes.set(dept.id, { key: dept.id, title: dept.name, children: [] }),
    )
    const roots: DeptTreeNode[] = []
    depts.value.forEach((dept) => {
        const node = nodes.get(dept.id)
        if (!node) return
        const parent = nodes.get(dept.parentId ?? 0)
        if (parent) parent.children.push(node)
        else roots.push(node)
    })
    return roots
})
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    return value
        ? rows.value.filter((row) =>
              `${row.username} ${row.nickname} ${row.email ?? ''}`
                  .toLowerCase()
                  .includes(value),
          )
        : rows.value
})

async function load() {
    loading.value = true
    try {
        const response = await userApi.page(
            page.value,
            size.value,
            selectedDeptId.value,
        )
        rows.value = response.data.data.records
        total.value = response.data.data.total
    } finally {
        loading.value = false
    }
}
function selectDept(selectedKeys: (string | number)[]) {
    selectedDeptId.value =
        selectedKeys[0] == null ? undefined : Number(selectedKeys[0])
    page.value = 1
    void load()
}
function changePage(pagination: { current?: number; pageSize?: number }) {
    page.value = pagination.current ?? 1
    size.value = pagination.pageSize ?? 10
    void load()
}
type UserTableColumn = {
    key: string
    dataIndex?: string
    title: string
    width?: number
    minWidth?: number
    align?: 'left' | 'center' | 'right'
}
const userTableColumns = computed<UserTableColumn[]>(() =>
    columnSettings.value
        .filter((column) => column.visible)
        .map((column) => ({
            key: column.field,
            dataIndex: column.field === 'operate' ? undefined : column.field,
            title: column.title,
            width:
                column.field === 'status'
                    ? 90
                    : column.field === 'operate'
                      ? 88
                      : undefined,
            minWidth:
                column.field === 'username' || column.field === 'nickname'
                    ? 140
                    : column.field === 'email'
                      ? 210
                      : column.field === 'deptId'
                        ? 140
                        : undefined,
            align: column.align,
        })),
)
function userRecord(record: unknown) {
    return record as User
}

async function loadOptions() {
    const [roleResponse, deptResponse] = await Promise.all([
        roleApi.page(1, 100),
        deptApi.page(1, 100),
    ])
    roles.value = roleResponse.data.data.records
    depts.value = deptResponse.data.data.records
}

function openCreate() {
    editingId.value = undefined
    Object.assign(form, emptyForm())
    editorOpen.value = true
}

function openEdit(row: User) {
    editingId.value = row.id
    Object.assign(form, {
        username: row.username,
        password: '',
        nickname: row.nickname,
        email: row.email ?? '',
        phone: row.phone ?? '',
        deptId: row.deptId,
        status: row.status,
    })
    editorOpen.value = true
}

async function save() {
    if (editingId.value) {
        await userApi.update(editingId.value, {
            nickname: form.nickname,
            email: form.email || undefined,
            phone: form.phone || undefined,
            deptId: form.deptId,
            status: form.status,
        })
    } else {
        await userApi.create({
            username: form.username,
            password: form.password,
            nickname: form.nickname,
            email: form.email || undefined,
            phone: form.phone || undefined,
            deptId: form.deptId,
        })
    }
    message.success('保存成功')
    editorOpen.value = false
    await load()
}

async function submitUserForm() {
    await formRef.value?.validate()
    await save()
}

function remove(row: User) {
    Modal.confirm({
        title: `删除用户“${row.username}”？`,
        content: '删除后该账号将无法登录。',
        okType: 'danger',
        async onOk() {
            await userApi.delete(row.id)
            message.success('删除成功')
            await load()
        },
    })
}
function kickout(row: User) {
    Modal.confirm({
        title: `强制“${row.username}”退出登录？`,
        async onOk() {
            await userApi.kickout(row.id)
            message.success('已踢下线')
        },
    })
}

function openRoles(row: User) {
    assigningUser.value = row
    selectedRoleIds.value = [...row.roleIds]
    roleOpen.value = true
}

function openResetPassword(row: User) {
    resettingUser.value = row
    resetPasswordForm.newPassword = ''
    resetPasswordForm.confirmPassword = ''
    resetPasswordOpen.value = true
}

async function saveResetPassword() {
    try {
        await resetPasswordFormRef.value?.validate()
        if (!resettingUser.value) return
        await userApi.resetPassword(
            resettingUser.value.id,
            resetPasswordForm.newPassword,
        )
        message.success('密码已重置，目标用户已下线并需使用新密码登录')
        resetPasswordOpen.value = false
    } catch (error) {
        if (!axios.isAxiosError(error)) {
            message.error('请检查密码填写是否正确')
        }
    }
}

function validatePasswordConfirmation(_rule: unknown, value: string) {
    return value === resetPasswordForm.newPassword
        ? Promise.resolve()
        : Promise.reject(new Error('两次输入的密码不一致'))
}

async function saveRoles() {
    if (!assigningUser.value) return
    await userApi.assignRoles(assigningUser.value.id, selectedRoleIds.value)
    message.success('角色已更新')
    roleOpen.value = false
    await load()
}

onMounted(async () => {
    await Promise.all([load(), loadOptions()])
})
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>用户管理</h1>
                <p>维护登录账号、状态和角色关系</p>
            </div>
            <a-space wrap>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'system:user:create'"
                    type="primary"
                    @click="openCreate"
                    ><PlusOutlined />新建用户</a-button
                >
            </a-space>
        </div>
        <div class="user-management-workspace">
            <aside class="user-dept-filter">
                <div class="user-dept-filter-title">部门范围</div>
                <a-tree
                    :tree-data="deptTreeData"
                    :selected-keys="selectedDeptId ? [selectedDeptId] : []"
                    @select="selectDept"
                />
            </aside>
            <div>
                <div class="page-toolbar">
                    <a-input-search
                        v-model:value="keyword"
                        allow-clear
                        placeholder="搜索账号、昵称或邮箱"
                        class="toolbar-search"
                    />
                </div>
                <AlphaTableCard :loading="loading">
                    <template #toolbar>
                        <TableColumnSetting
                            v-model="columnSettings"
                            control="align"
                            :storage-key="tableSettingsKey"
                        />
                    </template>
                    <a-table
                        row-key="id"
                        :data-source="filteredRows"
                        :columns="userTableColumns"
                        :pagination="false"
                        :scroll="{ x: 'max-content' }"
                    >
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'email'">
                                {{ record.email || '-' }}
                            </template>
                            <template v-else-if="column.key === 'deptId'">
                                {{
                                    depts.find(
                                        (item) => item.id === record.deptId,
                                    )?.name || '-'
                                }}
                            </template>
                            <template v-else-if="column.key === 'status'">
                                <a-badge
                                    :status="
                                        record.status === 1
                                            ? 'success'
                                            : 'default'
                                    "
                                    :text="
                                        record.status === 1 ? '启用' : '停用'
                                    "
                                />
                            </template>
                            <template v-else-if="column.key === 'operate'">
                                <a-tag
                                    v-if="record.username === 'admin'"
                                    color="blue"
                                    >内置管理员</a-tag
                                >
                                <TableActionMenu v-else aria-label="用户操作">
                                    <a-menu-item
                                        key="edit"
                                        v-permission="'system:user:update'"
                                        @click="openEdit(userRecord(record))"
                                        ><EditOutlined />编辑</a-menu-item
                                    >
                                    <a-menu-item
                                        v-if="
                                            record.id !==
                                            authStore.state.profile?.id
                                        "
                                        key="reset-password"
                                        v-permission="
                                            'system:user:reset-password'
                                        "
                                        @click="
                                            openResetPassword(
                                                userRecord(record),
                                            )
                                        "
                                        ><KeyOutlined />重置密码</a-menu-item
                                    >
                                    <a-menu-item
                                        key="roles"
                                        v-permission="'system:role:assign'"
                                        @click="openRoles(userRecord(record))"
                                        ><SafetyOutlined />角色</a-menu-item
                                    >
                                    <a-menu-item
                                        key="kickout"
                                        v-permission="'system:user:update'"
                                        @click="kickout(userRecord(record))"
                                        ><DisconnectOutlined />下线</a-menu-item
                                    >
                                    <a-menu-item
                                        key="delete"
                                        v-permission="'system:user:delete'"
                                        danger
                                        @click="remove(userRecord(record))"
                                        ><DeleteOutlined />删除</a-menu-item
                                    >
                                </TableActionMenu>
                            </template>
                        </template>
                    </a-table>
                    <template #footer>
                        <a-pagination
                            :current="page"
                            :page-size="size"
                            :total="total"
                            show-size-changer
                            :show-total="(count) => `共 ${count} 条`"
                            @change="
                                (current, pageSize) =>
                                    changePage({ current, pageSize })
                            "
                        />
                    </template>
                </AlphaTableCard>
            </div>
        </div>

        <a-modal
            v-model:open="editorOpen"
            :title="editingId ? '编辑用户' : '新建用户'"
            ok-text="保存"
            cancel-text="取消"
            @ok="submitUserForm"
        >
            <a-form
                ref="formRef"
                :model="form"
                :rules="rules"
                layout="vertical"
                autocomplete="off"
            >
                <a-form-item label="账号" name="username" required
                    ><a-input
                        v-model:value="form.username"
                        :disabled="!!editingId"
                        autocomplete="off"
                /></a-form-item>
                <a-form-item
                    v-if="!editingId"
                    label="初始密码"
                    name="password"
                    required
                    ><a-input-password
                        v-model:value="form.password"
                        autocomplete="new-password"
                /></a-form-item>
                <a-form-item label="昵称" name="nickname" required
                    ><a-input v-model:value="form.nickname"
                /></a-form-item>
                <div class="form-grid">
                    <a-form-item label="邮箱" name="email"
                        ><a-input v-model:value="form.email" /></a-form-item
                    ><a-form-item label="手机" name="phone"
                        ><a-input v-model:value="form.phone" :maxlength="11"
                    /></a-form-item>
                </div>
                <div class="form-grid">
                    <a-form-item label="部门"
                        ><a-select
                            v-model:value="form.deptId"
                            allow-clear
                            :options="
                                depts.map((item) => ({
                                    label: item.name,
                                    value: item.id,
                                }))
                            " /></a-form-item
                    ><a-form-item v-if="editingId" label="状态"
                        ><a-switch
                            v-model:checked="form.status"
                            :checked-value="1"
                            :un-checked-value="0"
                    /></a-form-item>
                </div>
            </a-form>
        </a-modal>
        <a-modal
            v-model:open="roleOpen"
            title="分配角色"
            ok-text="保存"
            cancel-text="取消"
            @ok="saveRoles"
        >
            <a-checkbox-group
                v-model:value="selectedRoleIds"
                class="assignment-list"
                :options="
                    roles.map((item) => ({
                        label: `${item.name} (${item.code})`,
                        value: item.id,
                    }))
                "
            />
        </a-modal>
        <a-modal
            v-model:open="resetPasswordOpen"
            :title="`重置 ${resettingUser?.username ?? ''} 的密码`"
            ok-text="确认重置"
            cancel-text="取消"
            @ok="saveResetPassword"
        >
            <a-alert
                type="warning"
                show-icon
                message="重置后会立即使该用户下线，并要求其使用新密码重新登录。"
                class="form-alert"
            />
            <a-form
                ref="resetPasswordFormRef"
                :model="resetPasswordForm"
                layout="vertical"
                autocomplete="off"
            >
                <a-form-item
                    label="新密码"
                    name="newPassword"
                    :rules="[
                        {
                            required: true,
                            min: 8,
                            message: '新密码至少为 8 位',
                        },
                    ]"
                    ><a-input-password
                        v-model:value="resetPasswordForm.newPassword"
                        autocomplete="new-password"
                /></a-form-item>
                <a-form-item
                    label="确认新密码"
                    name="confirmPassword"
                    :rules="[
                        {
                            required: true,
                            validator: validatePasswordConfirmation,
                        },
                    ]"
                    ><a-input-password
                        v-model:value="resetPasswordForm.confirmPassword"
                        autocomplete="new-password"
                /></a-form-item>
            </a-form>
        </a-modal>
    </section>
</template>

<style scoped>
.user-management-workspace {
    display: grid;
    grid-template-columns: minmax(180px, 240px) minmax(0, 1fr);
    gap: 16px;
}

.user-dept-filter {
    padding: 12px;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: var(--alpha-surface);
}

.user-dept-filter-title {
    margin-bottom: 10px;
    font-weight: 600;
}

@media (max-width: 767px) {
    .user-management-workspace {
        grid-template-columns: 1fr;
    }
}
</style>
