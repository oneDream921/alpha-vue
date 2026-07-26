<script setup lang="ts">
import {
    DeleteOutlined,
    DisconnectOutlined,
    EditOutlined,
    KeyOutlined,
    MoreOutlined,
    PlusOutlined,
    ReloadOutlined,
    SafetyOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import axios from 'axios'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

import {
    deptApi,
    roleApi,
    userApi,
    type Dept,
    type Role,
    type User,
} from '@/service/system'
import { authStore } from '@/stores/auth'

const rows = ref<User[]>([])
const roles = ref<Role[]>([])
const depts = ref<Dept[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
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
const viewportWidth = ref(
    typeof window === 'undefined' ? 1024 : window.innerWidth,
)
const showCompactActions = computed(() => viewportWidth.value < 768)
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
        const response = await userApi.page(page.value, size.value)
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

function updateViewport() {
    viewportWidth.value = window.innerWidth
}

onMounted(async () => {
    window.addEventListener('resize', updateViewport)
    await Promise.all([load(), loadOptions()])
})

onBeforeUnmount(() => window.removeEventListener('resize', updateViewport))
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
        <div class="page-toolbar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索账号、昵称或邮箱"
                class="toolbar-search"
            />
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
            :scroll="{ x: 940 }"
            @change="changePage"
        >
            <a-table-column title="账号" data-index="username" width="140" />
            <a-table-column title="昵称" data-index="nickname" width="140" />
            <a-table-column title="邮箱" data-index="email" width="210"
                ><template #default="{ text }">{{
                    text || '-'
                }}</template></a-table-column
            >
            <a-table-column title="部门" data-index="deptId" width="140"
                ><template #default="{ text }">{{
                    depts.find((item) => item.id === text)?.name || '-'
                }}</template></a-table-column
            >
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
            <a-table-column
                title="操作"
                :fixed="showCompactActions ? undefined : 'right'"
                :width="showCompactActions ? 84 : 164"
                align="center"
                ><template #default="{ record }"
                    ><div v-if="!showCompactActions" class="table-actions">
                        <a-tag v-if="record.username === 'admin'" color="blue"
                            >内置管理员</a-tag
                        >
                        <a-button
                            v-if="record.username !== 'admin'"
                            v-permission="'system:user:update'"
                            type="link"
                            size="small"
                            @click="openEdit(record)"
                            ><EditOutlined />编辑</a-button
                        >
                        <a-button
                            v-if="
                                record.username !== 'admin' &&
                                record.id !== authStore.state.profile?.id
                            "
                            v-permission="'system:user:reset-password'"
                            type="link"
                            size="small"
                            @click="openResetPassword(record)"
                            ><KeyOutlined />重置密码</a-button
                        >
                        <a-button
                            v-if="record.username !== 'admin'"
                            v-permission="'system:role:assign'"
                            type="link"
                            size="small"
                            @click="openRoles(record)"
                            ><SafetyOutlined />角色</a-button
                        >
                        <a-button
                            v-if="record.username !== 'admin'"
                            v-permission="'system:user:update'"
                            type="link"
                            size="small"
                            @click="kickout(record)"
                            ><DisconnectOutlined />下线</a-button
                        >
                        <a-button
                            v-if="record.username !== 'admin'"
                            v-permission="'system:user:delete'"
                            type="link"
                            danger
                            size="small"
                            @click="remove(record)"
                            ><DeleteOutlined />删除</a-button
                        >
                    </div>
                    <a-tag v-else-if="record.username === 'admin'" color="blue"
                        >内置管理员</a-tag
                    >
                    <a-dropdown
                        v-else
                        placement="bottomRight"
                        :trigger="['click']"
                    >
                        <a-button
                            type="link"
                            size="small"
                            aria-label="用户操作"
                        >
                            <MoreOutlined />操作
                        </a-button>
                        <template #overlay>
                            <a-menu class="mobile-action-menu">
                                <a-menu-item
                                    v-if="
                                        record.username !== 'admin' &&
                                        authStore.hasPermission(
                                            'system:user:update',
                                        )
                                    "
                                    key="edit"
                                    @click="openEdit(record)"
                                    ><EditOutlined />编辑</a-menu-item
                                >
                                <a-menu-item
                                    v-if="
                                        record.username !== 'admin' &&
                                        record.id !==
                                            authStore.state.profile?.id &&
                                        authStore.hasPermission(
                                            'system:user:reset-password',
                                        )
                                    "
                                    key="reset-password"
                                    @click="openResetPassword(record)"
                                    ><KeyOutlined />重置密码</a-menu-item
                                >
                                <a-menu-item
                                    v-if="
                                        record.username !== 'admin' &&
                                        authStore.hasPermission(
                                            'system:role:assign',
                                        )
                                    "
                                    key="roles"
                                    @click="openRoles(record)"
                                    ><SafetyOutlined />角色</a-menu-item
                                >
                                <a-menu-item
                                    v-if="
                                        record.username !== 'admin' &&
                                        authStore.hasPermission(
                                            'system:user:update',
                                        )
                                    "
                                    key="kickout"
                                    @click="kickout(record)"
                                    ><DisconnectOutlined />下线</a-menu-item
                                >
                                <a-menu-item
                                    v-if="
                                        record.username !== 'admin' &&
                                        authStore.hasPermission(
                                            'system:user:delete',
                                        )
                                    "
                                    key="delete"
                                    danger
                                    @click="remove(record)"
                                    ><DeleteOutlined />删除</a-menu-item
                                >
                            </a-menu>
                        </template>
                    </a-dropdown></template
                ></a-table-column
            >
        </a-table>

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
