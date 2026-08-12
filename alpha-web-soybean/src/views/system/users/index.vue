<script setup lang="ts">
/* eslint-disable no-nested-ternary */
import { computed, onMounted, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import {
  DeleteOutlined,
  DisconnectOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { type Dept, type Role, type User, deptApi, roleApi, userApi } from '@/service/api/system';
import TableActionMenu from '@/components/TableActionMenu.vue';
import AlphaTableCard from '@/components/AlphaTableCard.vue';
import AlphaTableColumnSetting, { type AlphaTableColumnSettingItem } from '@/components/AlphaTableColumnSetting.vue';
import { authStore } from '@/stores/auth';
import { toggleDeptSelection } from './users.dept-selection';

const rows = ref<User[]>([]);
const roles = ref<Role[]>([]);
const depts = ref<Dept[]>([]);
const loading = ref(false);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');
const selectedDeptId = ref<number>();
const editorOpen = ref(false);
const roleOpen = ref(false);
const resetPasswordOpen = ref(false);
const editingId = ref<number>();
const assigningUser = ref<User>();
const selectedRoleIds = ref<number[]>([]);
const resettingUser = ref<User>();
const resetPasswordFormRef = ref();
const resetPasswordForm = reactive({ newPassword: '', confirmPassword: '' });
const formRef = ref();
const columnSettings = ref<AlphaTableColumnSettingItem[]>([
  { field: 'username', title: '账号', visible: true, align: 'left' },
  { field: 'nickname', title: '昵称', visible: true, align: 'left' },
  { field: 'email', title: '邮箱', visible: true, align: 'left' },
  { field: 'deptId', title: '部门', visible: true, align: 'left' },
  { field: 'status', title: '状态', visible: true, align: 'center' },
  { field: 'operate', title: '操作', visible: true, align: 'center' }
]);
const tableSettingsKey = 'alpha-vue:table:system-users:v1';
const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入账号' },
    { max: 64, message: '账号不能超过 64 个字符' }
  ],
  password: [
    { required: true, message: '请输入初始密码' },
    { min: 8, message: '初始密码至少为 8 位' }
  ],
  nickname: [
    { required: true, message: '请输入昵称' },
    { max: 64, message: '昵称不能超过 64 个字符' }
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入 11 位中国大陆手机号' }]
};

const emptyForm = () => ({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: undefined as number | undefined,
  status: 1
});
const form = reactive(emptyForm());
type DeptTreeNode = { key: number; title: string; children: DeptTreeNode[] };
const deptTreeData = computed<DeptTreeNode[]>(() => {
  const nodes = new Map<number, DeptTreeNode>();
  depts.value.forEach(dept => nodes.set(dept.id, { key: dept.id, title: dept.name, children: [] }));
  const roots: DeptTreeNode[] = [];
  depts.value.forEach(dept => {
    const node = nodes.get(dept.id);
    if (!node) return;
    const parent = nodes.get(dept.parentId ?? 0);
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return roots;
});
const selectedDeptName = computed(() => depts.value.find(dept => dept.id === selectedDeptId.value)?.name);
const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return value
    ? rows.value.filter(row => `${row.username} ${row.nickname} ${row.email ?? ''}`.toLowerCase().includes(value))
    : rows.value;
});

async function load() {
  loading.value = true;
  try {
    const response = await userApi.page(page.value, size.value, selectedDeptId.value);
    rows.value = response.data?.records ?? [];
    total.value = response.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}
function selectDept(selectedKeys: (string | number)[]) {
  selectedDeptId.value = toggleDeptSelection(selectedDeptId.value, selectedKeys);
  page.value = 1;
  load();
}
function changePage(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current ?? 1;
  size.value = pagination.pageSize ?? 10;
  load();
}
type UserTableColumn = {
  key: string;
  dataIndex?: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
};
const userTableColumns = computed<UserTableColumn[]>(() =>
  columnSettings.value
    .filter(column => column.visible)
    .map(column => ({
      key: column.field,
      dataIndex: column.field === 'operate' ? undefined : column.field,
      title: column.title,
      width: column.field === 'status' ? 90 : column.field === 'operate' ? 88 : undefined,
      minWidth:
        column.field === 'username' || column.field === 'nickname'
          ? 140
          : column.field === 'email'
            ? 210
            : column.field === 'deptId'
              ? 140
              : undefined,
      align: column.align
    }))
);
function userRecord(record: unknown) {
  return record as User;
}

async function loadOptions() {
  const [roleResponse, deptResponse] = await Promise.all([roleApi.page(1, 100), deptApi.page(1, 100)]);
  roles.value = roleResponse.data?.records ?? [];
  depts.value = deptResponse.data?.records ?? [];
}

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, emptyForm());
  editorOpen.value = true;
}

function openEdit(row: User) {
  editingId.value = row.id;
  Object.assign(form, {
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email ?? '',
    phone: row.phone ?? '',
    deptId: row.deptId,
    status: row.status
  });
  editorOpen.value = true;
}

async function save() {
  if (editingId.value) {
    await userApi.update(editingId.value, {
      nickname: form.nickname,
      email: form.email || undefined,
      phone: form.phone || undefined,
      deptId: form.deptId,
      status: form.status
    });
  } else {
    await userApi.create({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email || undefined,
      phone: form.phone || undefined,
      deptId: form.deptId
    });
  }
  message.success('保存成功');
  editorOpen.value = false;
  await load();
}

async function submitUserForm() {
  await formRef.value?.validate();
  await save();
}

function remove(row: User) {
  Modal.confirm({
    title: `删除用户“${row.username}”？`,
    content: '删除后该账号将无法登录。',
    okType: 'danger',
    async onOk() {
      await userApi.delete(row.id);
      message.success('删除成功');
      await load();
    }
  });
}
function kickout(row: User) {
  Modal.confirm({
    title: `强制“${row.username}”退出登录？`,
    async onOk() {
      await userApi.kickout(row.id);
      message.success('已踢下线');
    }
  });
}

function openRoles(row: User) {
  assigningUser.value = row;
  selectedRoleIds.value = [...row.roleIds];
  roleOpen.value = true;
}

function openResetPassword(row: User) {
  resettingUser.value = row;
  resetPasswordForm.newPassword = '';
  resetPasswordForm.confirmPassword = '';
  resetPasswordOpen.value = true;
}

async function saveResetPassword() {
  try {
    await resetPasswordFormRef.value?.validate();
    if (!resettingUser.value) return;
    await userApi.resetPassword(resettingUser.value.id, resetPasswordForm.newPassword);
    message.success('密码已重置，目标用户已下线并需使用新密码登录');
    resetPasswordOpen.value = false;
  } catch {
    message.error('密码重置失败，请检查密码填写是否正确');
  }
}

function validatePasswordConfirmation(_rule: unknown, value: string) {
  return value === resetPasswordForm.newPassword
    ? Promise.resolve()
    : Promise.reject(new Error('两次输入的密码不一致'));
}

async function saveRoles() {
  if (!assigningUser.value) return;
  await userApi.assignRoles(assigningUser.value.id, selectedRoleIds.value);
  message.success('角色已更新');
  roleOpen.value = false;
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()]);
});
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>用户管理</h1>
        <p>维护登录账号、状态和角色关系</p>
      </div>
      <ASpace wrap>
        <AButton @click="load">
          <ReloadOutlined />
          刷新
        </AButton>
        <AButton v-permission="'system:user:create'" type="primary" @click="openCreate">
          <PlusOutlined />
          新建用户
        </AButton>
      </ASpace>
    </div>
    <div class="user-management-workspace">
      <aside class="user-dept-filter">
        <div class="user-dept-filter-title">
          <span>部门范围</span>
          <ATag v-if="selectedDeptName" color="blue" bordered>
            {{ selectedDeptName }}
          </ATag>
        </div>
        <p class="user-dept-filter-hint">点击部门筛选，再次点击可取消</p>
        <ATree
          class="user-dept-tree"
          block-node
          show-line
          :tree-data="deptTreeData"
          :selected-keys="selectedDeptId ? [selectedDeptId] : []"
          @select="selectDept"
        >
          <template #title="{ title, key }">
            <span
              class="user-dept-tree-label"
              :class="{
                'user-dept-tree-label-selected': Number(key) === selectedDeptId
              }"
            >
              <span v-if="Number(key) === selectedDeptId" class="user-dept-tree-label-marker" aria-hidden="true" />
              <span>{{ title }}</span>
            </span>
          </template>
        </ATree>
      </aside>
      <div>
        <div class="page-toolbar">
          <AInputSearch v-model:value="keyword" allow-clear placeholder="搜索账号、昵称或邮箱" class="toolbar-search" />
        </div>
        <AlphaTableCard :loading="loading">
          <template #toolbar>
            <AlphaTableColumnSetting v-model="columnSettings" control="align" :storage-key="tableSettingsKey" />
          </template>
          <ATable
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
                {{ depts.find(item => item.id === record.deptId)?.name || '-' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <ABadge
                  :status="record.status === 1 ? 'success' : 'default'"
                  :text="record.status === 1 ? '启用' : '停用'"
                />
              </template>
              <template v-else-if="column.key === 'operate'">
                <ATag v-if="record.username === 'admin'" color="blue">内置管理员</ATag>
                <TableActionMenu v-else aria-label="用户操作">
                  <AMenuItem key="edit" v-permission="'system:user:update'" @click="openEdit(userRecord(record))">
                    <EditOutlined />
                    编辑
                  </AMenuItem>
                  <AMenuItem
                    v-if="record.id !== authStore.state.profile?.id"
                    key="reset-password"
                    v-permission="'system:user:reset-password'"
                    @click="openResetPassword(userRecord(record))"
                  >
                    <KeyOutlined />
                    重置密码
                  </AMenuItem>
                  <AMenuItem key="roles" v-permission="'system:role:assign'" @click="openRoles(userRecord(record))">
                    <SafetyOutlined />
                    角色
                  </AMenuItem>
                  <AMenuItem key="kickout" v-permission="'system:user:update'" @click="kickout(userRecord(record))">
                    <DisconnectOutlined />
                    下线
                  </AMenuItem>
                  <AMenuItem
                    key="delete"
                    v-permission="'system:user:delete'"
                    danger
                    @click="remove(userRecord(record))"
                  >
                    <DeleteOutlined />
                    删除
                  </AMenuItem>
                </TableActionMenu>
              </template>
            </template>
          </ATable>
          <template #footer>
            <APagination
              :current="page"
              :page-size="size"
              :total="total"
              show-size-changer
              :show-total="count => `共 ${count} 条`"
              @change="(current, pageSize) => changePage({ current, pageSize })"
            />
          </template>
        </AlphaTableCard>
      </div>
    </div>

    <AModal
      v-model:open="editorOpen"
      :title="editingId ? '编辑用户' : '新建用户'"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitUserForm"
    >
      <AForm ref="formRef" :model="form" :rules="rules" layout="vertical" autocomplete="off">
        <AFormItem label="账号" name="username" required>
          <AInput v-model:value="form.username" :disabled="!!editingId" autocomplete="off" />
        </AFormItem>
        <AFormItem v-if="!editingId" label="初始密码" name="password" required>
          <AInputPassword v-model:value="form.password" autocomplete="new-password" />
        </AFormItem>
        <AFormItem label="昵称" name="nickname" required><AInput v-model:value="form.nickname" /></AFormItem>
        <div class="form-grid">
          <AFormItem label="邮箱" name="email"><AInput v-model:value="form.email" /></AFormItem>
          <AFormItem label="手机" name="phone"><AInput v-model:value="form.phone" :maxlength="11" /></AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="部门">
            <ASelect
              v-model:value="form.deptId"
              allow-clear
              :options="
                depts.map(item => ({
                  label: item.name,
                  value: item.id
                }))
              "
            />
          </AFormItem>
          <AFormItem v-if="editingId" label="状态">
            <ASwitch v-model:checked="form.status" :checked-value="1" :un-checked-value="0" />
          </AFormItem>
        </div>
      </AForm>
    </AModal>
    <AModal v-model:open="roleOpen" title="分配角色" ok-text="保存" cancel-text="取消" @ok="saveRoles">
      <ACheckboxGroup
        v-model:value="selectedRoleIds"
        class="assignment-list"
        :options="
          roles.map(item => ({
            label: `${item.name} (${item.code})`,
            value: item.id
          }))
        "
      />
    </AModal>
    <AModal
      v-model:open="resetPasswordOpen"
      :title="`重置 ${resettingUser?.username ?? ''} 的密码`"
      ok-text="确认重置"
      cancel-text="取消"
      @ok="saveResetPassword"
    >
      <AAlert
        type="warning"
        show-icon
        message="重置后会立即使该用户下线，并要求其使用新密码重新登录。"
        class="form-alert"
      />
      <AForm ref="resetPasswordFormRef" :model="resetPasswordForm" layout="vertical" autocomplete="off">
        <AFormItem
          label="新密码"
          name="newPassword"
          :rules="[
            {
              required: true,
              min: 8,
              message: '新密码至少为 8 位'
            }
          ]"
        >
          <AInputPassword v-model:value="resetPasswordForm.newPassword" autocomplete="new-password" />
        </AFormItem>
        <AFormItem
          label="确认新密码"
          name="confirmPassword"
          :rules="[
            {
              required: true,
              validator: validatePasswordConfirmation
            }
          ]"
        >
          <AInputPassword v-model:value="resetPasswordForm.confirmPassword" autocomplete="new-password" />
        </AFormItem>
      </AForm>
    </AModal>
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-weight: 600;
}

.user-dept-filter-title .ant-tag {
  max-width: 132px;
  margin-inline-end: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dept-filter-hint {
  margin: -4px 0 10px;
  color: var(--alpha-muted);
  font-size: 12px;
}

:deep(.user-dept-tree .ant-tree-treenode) {
  width: 100%;
  padding-block: 2px;
}

:deep(.user-dept-tree .ant-tree-switcher-line-icon) {
  transform: translateY(3px);
}

:deep(.user-dept-tree .ant-tree-switcher-leaf-line) {
  position: relative;
  top: 2px;
}

:deep(.user-dept-tree .ant-tree-switcher-noop .anticon-file) {
  transform: translateY(3px);
}

:deep(.user-dept-tree .ant-tree-node-content-wrapper) {
  min-height: 34px;
  padding: 6px 8px;
  line-height: 20px;
  border-radius: 6px;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

:deep(.user-dept-tree .ant-tree-node-content-wrapper:hover) {
  color: var(--alpha-primary-strong) !important;
  background: var(--alpha-primary-soft) !important;
}

:deep(.user-dept-tree .ant-tree-node-content-wrapper.ant-tree-node-selected) {
  color: var(--alpha-primary-strong) !important;
  font-weight: 700 !important;
  background-color: var(--alpha-primary-soft) !important;
  box-shadow: inset 4px 0 0 var(--alpha-primary) !important;
}

:deep(.user-dept-tree .ant-tree-node-content-wrapper.ant-tree-node-selected .ant-tree-title) {
  color: inherit !important;
}

:deep(.user-dept-tree-label) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 20px;
  line-height: 20px;
  vertical-align: middle;
}

:deep(.user-dept-tree-label-marker) {
  display: block;
  position: relative;
  top: 2px;
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: var(--alpha-primary-strong);
}

:deep(.user-dept-tree-label-selected) {
  color: var(--alpha-primary-strong) !important;
  font-weight: 700 !important;
}

@media (max-width: 767px) {
  .user-management-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
