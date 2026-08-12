<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { fetchGetBackendRoutes } from '@/service/api/route';
import { type Menu, menuApi } from '@/service/api/system';
import AlphaTableCard from '@/components/AlphaTableCard.vue';
import { authStore } from '@/stores/auth';

const rows = ref<Menu[]>([]);
const loading = ref(false);
const editorOpen = ref(false);
const editingId = ref<number>();
const formRef = ref();
const expandedRowKeys = ref<number[]>([]);
const keyword = ref('');
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
  status: 1
});
const form = reactive(emptyForm());
const rules: Record<string, Rule[]> = {
  title: [
    { required: true, whitespace: true, message: '请输入菜单名称' },
    { max: 64, message: '菜单名称不能超过 64 个字符' }
  ],
  menuType: [{ required: true, message: '请选择菜单类型' }],
  parentId: [{ type: 'number', min: 0, message: '请选择有效的上级菜单' }],
  sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }],
  path: [{ max: 128, message: '路由路径不能超过 128 个字符' }],
  component: [{ max: 255, message: '组件标识不能超过 255 个字符' }],
  permission: [{ max: 128, message: '权限编码不能超过 128 个字符' }],
  icon: [{ max: 64, message: '图标不能超过 64 个字符' }]
};

type MenuTreeNode = { title: string; value: number; children?: MenuTreeNode[] };
type MenuRow = Menu & { children: MenuRow[] };
function descendantIds(parentId: number, ids = new Set<number>()) {
  ids.add(parentId);
  rows.value.filter(item => item.parentId === parentId).forEach(item => descendantIds(item.id, ids));
  return ids;
}
const parentMenuOptions = computed<MenuTreeNode[]>(() => {
  const excluded = editingId.value ? descendantIds(editingId.value) : new Set();
  const nodes = new Map<number, MenuTreeNode>();
  rows.value
    .filter(item => !excluded.has(item.id))
    .forEach(item =>
      nodes.set(item.id, {
        title: item.title,
        value: item.id,
        children: []
      })
    );
  const roots: MenuTreeNode[] = [];
  rows.value.forEach(item => {
    const node = nodes.get(item.id);
    if (!node) return;
    const parent = nodes.get(item.parentId ?? 0);
    if (parent) parent.children?.push(node);
    else roots.push(node);
  });
  return roots;
});
const treeRows = computed(() => {
  const nodes = new Map<number, MenuRow>();
  rows.value.forEach(item => nodes.set(item.id, { ...item, children: [] }));
  const roots: MenuRow[] = [];
  nodes.forEach(node => {
    const parent = nodes.get(node.parentId ?? 0);
    if (parent) parent.children.push(node);
    else roots.push(node);
  });
  return roots;
});
const filteredTreeRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) return treeRows.value;
  const include = (node: MenuRow): MenuRow | null => {
    const children = node.children.map(include).filter((item): item is MenuRow => item !== null);
    const text = `${node.title} ${node.path ?? ''} ${node.permission ?? ''}`.toLowerCase();
    return text.includes(value) || children.length ? { ...node, children } : null;
  };
  return treeRows.value.map(include).filter((item): item is MenuRow => item !== null);
});
function menuTypeLabel(value: Menu['menuType']) {
  return { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮' }[value];
}

async function load() {
  loading.value = true;
  try {
    rows.value = (await menuApi.page(1, 100)).data?.records ?? [];
    expandedRowKeys.value = rows.value.map(item => item.id);
  } finally {
    loading.value = false;
  }
}
function openCreate() {
  editingId.value = undefined;
  Object.assign(form, emptyForm());
  editorOpen.value = true;
}
function openCreateChild(row: Menu) {
  editingId.value = undefined;
  Object.assign(form, { ...emptyForm(), parentId: row.id });
  editorOpen.value = true;
}
function expandAll() {
  expandedRowKeys.value = rows.value.map(item => item.id);
}
function collapseAll() {
  expandedRowKeys.value = [];
}
type MenuTableColumn = {
  key: string;
  dataIndex?: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
};
const menuTableColumns: MenuTableColumn[] = [
  { key: 'title', dataIndex: 'title', title: '名称', minWidth: 180 },
  {
    key: 'menuType',
    dataIndex: 'menuType',
    title: '类型',
    width: 110,
    align: 'center'
  },
  { key: 'path', dataIndex: 'path', title: '路由', minWidth: 160 },
  {
    key: 'component',
    dataIndex: 'component',
    title: '组件',
    minWidth: 180
  },
  {
    key: 'permission',
    dataIndex: 'permission',
    title: '权限编码',
    minWidth: 220
  },
  {
    key: 'sortOrder',
    dataIndex: 'sortOrder',
    title: '排序',
    width: 80,
    align: 'center'
  },
  {
    key: 'status',
    dataIndex: 'status',
    title: '状态',
    width: 90,
    align: 'center'
  },
  { key: 'operate', title: '操作', width: 150, align: 'center' }
];
function handleTableExpand(expanded: boolean, row: unknown) {
  const record = row as MenuRow;
  const next = new Set(expandedRowKeys.value);
  if (expanded) next.add(record.id);
  else next.delete(record.id);
  expandedRowKeys.value = [...next];
}
function menuRecord(record: unknown) {
  return record as Menu;
}
function openEdit(row: Menu) {
  editingId.value = row.id;
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
    status: row.status
  });
  editorOpen.value = true;
}
function payload() {
  return {
    ...form,
    parentId: form.parentId ?? 0,
    path: form.path || undefined,
    component: form.component || undefined,
    permission: form.permission || undefined,
    icon: form.icon || undefined
  };
}
async function submitForm() {
  await formRef.value?.validate();
  await save();
}
async function save() {
  if (editingId.value) await menuApi.update(editingId.value, payload());
  else await menuApi.create(payload());
  const routes = await fetchGetBackendRoutes();
  if (routes.data) authStore.setRoutes(routes.data);
  message.success('保存成功');
  editorOpen.value = false;
  await load();
}
function remove(row: Menu) {
  Modal.confirm({
    title: `删除菜单“${row.title}”？`,
    content: '存在子菜单时后端会拒绝删除。',
    okType: 'danger',
    async onOk() {
      await menuApi.delete(row.id);
      const routes = await fetchGetBackendRoutes();
      if (routes.data) authStore.setRoutes(routes.data);
      message.success('删除成功');
      await load();
    }
  });
}
onMounted(load);
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>菜单管理</h1>
        <p>维护导航、页面和按钮权限编码</p>
      </div>
      <ASpace wrap>
        <AButton @click="collapseAll">全部收起</AButton>
        <AButton @click="expandAll">全部展开</AButton>
        <AButton @click="load">
          <ReloadOutlined />
          刷新
        </AButton>
        <AButton v-permission="'system:menu:create'" type="primary" @click="openCreate">
          <PlusOutlined />
          新建菜单
        </AButton>
      </ASpace>
    </div>
    <div class="query-bar">
      <AInputSearch v-model:value="keyword" allow-clear placeholder="搜索菜单名称、路由或权限编码" />
      <AButton @click="keyword = ''">重置</AButton>
    </div>
    <AlphaTableCard :loading="loading">
      <ATable
        row-key="id"
        :data-source="filteredTreeRows"
        :columns="menuTableColumns"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        :scroll="{ x: 'max-content' }"
        @expand="handleTableExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <ATag>{{ menuTypeLabel(record.menuType) }}</ATag>
          </template>
          <template v-else-if="column.key === 'path'">
            {{ record.path || '-' }}
          </template>
          <template v-else-if="column.key === 'component'">
            {{ record.component || '-' }}
          </template>
          <template v-else-if="column.key === 'permission'">
            <ATypographyText v-if="record.permission" code>{{ record.permission }}</ATypographyText>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <ABadge
              :status="record.status === 1 ? 'success' : 'default'"
              :text="record.status === 1 ? '启用' : '停用'"
            />
          </template>
          <template v-else-if="column.key === 'operate'">
            <ASpace :size="4">
              <AButton
                v-permission="'system:menu:create'"
                type="text"
                size="small"
                title="新增子菜单"
                @click.stop="openCreateChild(menuRecord(record))"
              >
                <PlusOutlined />
              </AButton>
              <AButton
                v-permission="'system:menu:update'"
                type="text"
                size="small"
                title="编辑菜单"
                @click.stop="openEdit(menuRecord(record))"
              >
                <EditOutlined />
              </AButton>
              <AButton
                v-permission="'system:menu:delete'"
                type="text"
                danger
                size="small"
                title="删除菜单"
                @click.stop="remove(menuRecord(record))"
              >
                <DeleteOutlined />
              </AButton>
            </ASpace>
          </template>
        </template>
      </ATable>
    </AlphaTableCard>
    <AModal
      v-model:open="editorOpen"
      :title="editingId ? '编辑菜单' : '新建菜单'"
      width="680px"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitForm"
    >
      <AForm ref="formRef" :model="form" :rules="rules" layout="vertical">
        <div class="form-grid">
          <AFormItem label="名称" name="title" required><AInput v-model:value="form.title" /></AFormItem>
          <AFormItem label="类型" name="menuType" required>
            <ASegmented
              v-model:value="form.menuType"
              :options="[
                { label: '目录', value: 'DIRECTORY' },
                { label: '菜单', value: 'MENU' },
                { label: '按钮', value: 'BUTTON' }
              ]"
            />
          </AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="上级菜单" name="parentId">
            <ATreeSelect
              v-model:value="form.parentId"
              class="full-width"
              :tree-data="parentMenuOptions"
              :field-names="{
                label: 'title',
                value: 'value',
                children: 'children'
              }"
              placeholder="不选则为顶级菜单"
              allow-clear
              tree-default-expand-all
            />
          </AFormItem>
          <AFormItem label="排序" name="sortOrder">
            <AInputNumber v-model:value="form.sortOrder" class="full-width" />
          </AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="路由路径" name="path"><AInput v-model:value="form.path" /></AFormItem>
          <AFormItem label="组件标识" name="component"><AInput v-model:value="form.component" /></AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="权限编码" name="permission"><AInput v-model:value="form.permission" /></AFormItem>
          <AFormItem label="图标" name="icon"><AInput v-model:value="form.icon" /></AFormItem>
        </div>
        <div class="form-grid">
          <AFormItem label="导航可见">
            <ASwitch v-model:checked="form.visible" :checked-value="1" :un-checked-value="0" />
          </AFormItem>
          <AFormItem label="状态">
            <ASwitch v-model:checked="form.status" :checked-value="1" :un-checked-value="0" />
          </AFormItem>
        </div>
      </AForm>
    </AModal>
  </section>
</template>
