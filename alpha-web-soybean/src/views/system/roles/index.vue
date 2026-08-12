<script setup lang="ts">
/* eslint-disable no-nested-ternary */
import { computed, onMounted, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SafetyOutlined } from '@ant-design/icons-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { type Menu, type Role, menuApi, roleApi } from '@/service/api/system';
import TableActionMenu from '@/components/TableActionMenu.vue';
import AlphaTableCard from '@/components/AlphaTableCard.vue';
import AlphaTableColumnSetting, { type AlphaTableColumnSettingItem } from '@/components/AlphaTableColumnSetting.vue';
import {
  buildRolePermissionTree,
  collectRolePermissionKeys,
  toggleRolePermission,
  withRolePermissionAncestors
} from './roles.permissions';

const rows = ref<Role[]>([]);
const menus = ref<Menu[]>([]);
const loading = ref(false);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');
const editorOpen = ref(false);
const permissionOpen = ref(false);
const editingId = ref<number>();
const formRef = ref();
const assigningRole = ref<Role>();
const selectedMenuIds = ref<Array<number | string>>([]);
const expandedMenuIds = ref<number[]>([]);
const columnSettings = ref<AlphaTableColumnSettingItem[]>([
  { field: 'name', title: '角色名称', visible: true, align: 'left' },
  { field: 'code', title: '编码', visible: true, align: 'left' },
  { field: 'sortOrder', title: '排序', visible: true, align: 'center' },
  { field: 'status', title: '状态', visible: true, align: 'center' },
  { field: 'remark', title: '备注', visible: true, align: 'left' },
  { field: 'operate', title: '操作', visible: true, align: 'center' }
]);
const tableSettingsKey = 'alpha-vue:table:system-roles:v1';
const emptyForm = () => ({
  name: '',
  code: '',
  sortOrder: 0,
  status: 1,
  remark: ''
});
const form = reactive(emptyForm());
const rules: Record<string, Rule[]> = {
  name: [
    { required: true, whitespace: true, message: '请输入角色名称' },
    { max: 64, message: '角色名称不能超过 64 个字符' }
  ],
  code: [
    { required: true, whitespace: true, message: '请输入角色编码' },
    {
      pattern: /^[A-Z][A-Z0-9_]*$/,
      message: '角色编码需以大写字母开头，仅含大写字母、数字和下划线'
    },
    { max: 64, message: '角色编码不能超过 64 个字符' }
  ],
  sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符' }]
};
const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return value
    ? rows.value.filter(item => `${item.name} ${item.code} ${item.remark ?? ''}`.toLowerCase().includes(value))
    : rows.value;
});
const permissionTreeData = computed(() => buildRolePermissionTree(menus.value));
const allPermissionIds = computed(() => collectRolePermissionKeys(permissionTreeData.value));

function normalizePermissionMenus(items: Menu[]): Menu[] {
  return items.map(item => ({
    ...item,
    id: Number(item.id),
    parentId: item.parentId === undefined ? undefined : Number(item.parentId)
  }));
}

async function load() {
  loading.value = true;
  try {
    const response = await roleApi.page(page.value, size.value);
    rows.value = response.data?.records ?? [];
    total.value = response.data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}
function changePage(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current ?? 1;
  size.value = pagination.pageSize ?? 10;
  load();
}
type RoleTableColumn = {
  key: string;
  dataIndex?: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
};
const roleTableColumns = computed<RoleTableColumn[]>(() =>
  columnSettings.value
    .filter(column => column.visible)
    .map(column => ({
      key: column.field,
      dataIndex: column.field === 'operate' ? undefined : column.field,
      title: column.title,
      width:
        column.field === 'sortOrder' || column.field === 'status' ? 90 : column.field === 'operate' ? 88 : undefined,
      minWidth: column.field === 'name' || column.field === 'code' ? 180 : column.field === 'remark' ? 220 : undefined,
      align: column.align
    }))
);
function editRoleRecord(record: unknown) {
  openEdit(record as Role);
}
function removeRoleRecord(record: unknown) {
  remove(record as Role);
}
function openPermissionsRecord(record: unknown) {
  openPermissions(record as Role);
}
function openCreate() {
  editingId.value = undefined;
  Object.assign(form, emptyForm());
  editorOpen.value = true;
}
function openEdit(row: Role) {
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name,
    code: row.code,
    sortOrder: row.sortOrder,
    status: row.status,
    remark: row.remark ?? ''
  });
  editorOpen.value = true;
}
async function save() {
  if (editingId.value)
    await roleApi.update(editingId.value, {
      name: form.name,
      sortOrder: form.sortOrder,
      status: form.status,
      remark: form.remark || undefined
    });
  else await roleApi.create({ ...form, remark: form.remark || undefined });
  message.success('保存成功');
  editorOpen.value = false;
  await load();
}
async function submitForm() {
  await formRef.value?.validate();
  await save();
}
function remove(row: Role) {
  Modal.confirm({
    title: `删除角色“${row.name}”？`,
    okType: 'danger',
    async onOk() {
      await roleApi.delete(row.id);
      message.success('删除成功');
      await load();
    }
  });
}
async function openPermissions(row: Role) {
  assigningRole.value = row;
  const [assignableMenus, menuIds] = await Promise.all([menuApi.assignable(), roleApi.menuIds(row.id)]);
  menus.value = normalizePermissionMenus(assignableMenus.data ?? []);
  selectedMenuIds.value = withRolePermissionAncestors(menus.value, menuIds.data ?? []);
  expandedMenuIds.value = allPermissionIds.value;
  permissionOpen.value = true;
}
function selectAllPermissions() {
  selectedMenuIds.value = [...allPermissionIds.value];
}
function clearPermissions() {
  selectedMenuIds.value = [];
}
function updatePermissionSelection(_keys: unknown, info: { node?: { key?: number | string }; checked?: boolean }) {
  togglePermissionFromTitle(info.node?.key, info.checked);
}
function togglePermissionFromTitle(key: number | string | undefined, checked?: boolean) {
  const numericKey = Number(key);
  if (!Number.isSafeInteger(numericKey)) return;
  const selectedKeys = selectedMenuIds.value.map(Number);
  const isSelected = selectedKeys.includes(numericKey);
  if (checked !== undefined && checked === isSelected) return;
  selectedMenuIds.value = toggleRolePermission(menus.value, selectedKeys, numericKey);
}
async function savePermissions() {
  if (!assigningRole.value) return;
  const menuIds = withRolePermissionAncestors(menus.value, selectedMenuIds.value.map(Number)).filter(
    Number.isSafeInteger
  );
  await roleApi.assignMenus(assigningRole.value.id, menuIds);
  message.success('权限已更新');
  permissionOpen.value = false;
}
onMounted(async () => {
  await load();
});
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>角色管理</h1>
        <p>配置角色状态和菜单按钮权限</p>
      </div>
      <ASpace wrap>
        <AButton @click="load">
          <ReloadOutlined />
          刷新
        </AButton>
        <AButton v-permission="'system:role:create'" type="primary" @click="openCreate">
          <PlusOutlined />
          新建角色
        </AButton>
      </ASpace>
    </div>
    <div class="query-bar">
      <AInputSearch v-model:value="keyword" allow-clear placeholder="搜索角色名称、编码或备注" />
      <AButton @click="keyword = ''">重置</AButton>
    </div>
    <AlphaTableCard :loading="loading">
      <template #toolbar>
        <AlphaTableColumnSetting v-model="columnSettings" control="align" :storage-key="tableSettingsKey" />
      </template>
      <ATable
        row-key="id"
        :data-source="filteredRows"
        :columns="roleTableColumns"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <ATypographyText code>{{ record.code }}</ATypographyText>
          </template>
          <template v-else-if="column.key === 'status'">
            <ABadge
              :status="record.status === 1 ? 'success' : 'default'"
              :text="record.status === 1 ? '启用' : '停用'"
            />
          </template>
          <template v-else-if="column.key === 'remark'">
            {{ record.remark || '-' }}
          </template>
          <template v-else-if="column.key === 'operate'">
            <TableActionMenu aria-label="角色操作">
              <AMenuItem key="edit" v-permission="'system:role:update'" @click="editRoleRecord(record)">
                <EditOutlined />
                编辑
              </AMenuItem>
              <AMenuItem key="permissions" v-permission="'system:role:assign'" @click="openPermissionsRecord(record)">
                <SafetyOutlined />
                权限
              </AMenuItem>
              <AMenuItem
                v-if="record.code !== 'SUPER_ADMIN'"
                key="delete"
                v-permission="'system:role:delete'"
                danger
                @click="removeRoleRecord(record)"
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
    <AModal
      v-model:open="editorOpen"
      :title="editingId ? '编辑角色' : '新建角色'"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitForm"
    >
      <AForm ref="formRef" :model="form" :rules="rules" layout="vertical">
        <div class="form-grid">
          <AFormItem label="名称" name="name" required><AInput v-model:value="form.name" /></AFormItem>
          <AFormItem label="编码" name="code" required>
            <AInput v-model:value="form.code" :disabled="!!editingId" />
          </AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="排序" name="sortOrder">
            <AInputNumber v-model:value="form.sortOrder" class="full-width" />
          </AFormItem>
          <AFormItem label="状态">
            <ASwitch v-model:checked="form.status" :checked-value="1" :un-checked-value="0" />
          </AFormItem>
        </div>
        <AFormItem label="备注" name="remark"><ATextarea v-model:value="form.remark" :rows="3" /></AFormItem>
      </AForm>
    </AModal>
    <AModal
      v-model:open="permissionOpen"
      :title="`分配菜单与按钮权限${assigningRole ? ` · ${assigningRole.name}` : ''}`"
      ok-text="保存"
      cancel-text="取消"
      width="720px"
      class="role-permission-modal"
      @ok="savePermissions"
    >
      <div class="permission-toolbar">
        <span>已选择 {{ selectedMenuIds.length }} 项</span>
        <ASpace size="small" wrap>
          <AButton type="link" size="small" @click="selectAllPermissions">全选</AButton>
          <AButton type="link" size="small" @click="clearPermissions">清空</AButton>
          <AButton type="link" size="small" @click="expandedMenuIds = allPermissionIds">展开全部</AButton>
          <AButton type="link" size="small" @click="expandedMenuIds = []">收起全部</AButton>
        </ASpace>
      </div>
      <ATree
        :checked-keys="selectedMenuIds"
        class="role-permission-tree"
        checkable
        check-strictly
        :expanded-keys="expandedMenuIds"
        :tree-data="permissionTreeData"
        @expand="keys => (expandedMenuIds = keys as number[])"
        @check="updatePermissionSelection"
      >
        <template #title="{ title, dataRef }">
          <span
            class="permission-tree-title"
            role="button"
            tabindex="0"
            @click.stop="togglePermissionFromTitle(dataRef.key)"
            @keydown.enter.stop="togglePermissionFromTitle(dataRef.key)"
            @keydown.space.prevent.stop="togglePermissionFromTitle(dataRef.key)"
          >
            {{ title }}
            <ATag class="ml-2" :color="dataRef.menuType === 'BUTTON' ? 'orange' : 'blue'">
              {{ dataRef.menuType === 'BUTTON' ? '按钮' : '菜单' }}
            </ATag>
            <span v-if="dataRef.permission" class="permission-code">{{ dataRef.permission }}</span>
          </span>
        </template>
      </ATree>
    </AModal>
  </section>
</template>
