<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { type Dept, deptApi } from '@/service/api/system';
import AlphaTableCard from '@/components/AlphaTableCard.vue';

const rows = ref<Dept[]>([]);
const loading = ref(false);
const editorOpen = ref(false);
const editingId = ref<number>();
const formRef = ref();
const expandedRowKeys = ref<number[]>([]);
const keyword = ref('');
const emptyForm = () => ({
  parentId: undefined as number | undefined,
  name: '',
  sortOrder: 0,
  status: 1
});
const form = reactive(emptyForm());
const rules: Record<string, Rule[]> = {
  name: [
    { required: true, whitespace: true, message: '请输入部门名称' },
    { max: 64, message: '部门名称不能超过 64 个字符' }
  ],
  parentId: [{ type: 'number', min: 0, message: '请选择有效的上级部门' }],
  sortOrder: [{ type: 'number', min: 0, message: '排序必须是非负整数' }]
};

type DeptTreeNode = { title: string; value: number; children?: DeptTreeNode[] };
type DeptRow = Dept & { children: DeptRow[] };

function descendantIds(parentId: number, ids = new Set<number>()) {
  ids.add(parentId);
  rows.value.filter(item => item.parentId === parentId).forEach(item => descendantIds(item.id, ids));
  return ids;
}

const parentDeptOptions = computed<DeptTreeNode[]>(() => {
  const excluded = editingId.value ? descendantIds(editingId.value) : new Set();
  const nodes = new Map<number, DeptTreeNode>();
  rows.value
    .filter(item => !excluded.has(item.id))
    .forEach(item =>
      nodes.set(item.id, {
        title: item.name,
        value: item.id,
        children: []
      })
    );
  const roots: DeptTreeNode[] = [];
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
  const nodes = new Map<number, DeptRow>();
  rows.value.forEach(item => nodes.set(item.id, { ...item, children: [] }));
  const roots: DeptRow[] = [];
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
  const include = (node: DeptRow): DeptRow | null => {
    const children = node.children.map(include).filter((item): item is DeptRow => item !== null);
    return node.name.toLowerCase().includes(value) || children.length ? { ...node, children } : null;
  };
  return treeRows.value.map(include).filter((item): item is DeptRow => item !== null);
});
async function load() {
  loading.value = true;
  try {
    rows.value = (await deptApi.page(1, 100)).data?.records ?? [];
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
function openCreateChild(row: Dept) {
  editingId.value = undefined;
  Object.assign(form, { ...emptyForm(), parentId: row.id });
  editorOpen.value = true;
}
function openCreateChildRecord(record: unknown) {
  openCreateChild(record as Dept);
}
function expandAll() {
  expandedRowKeys.value = rows.value.map(item => item.id);
}
function collapseAll() {
  expandedRowKeys.value = [];
}
type DeptTableColumn = {
  key: string;
  dataIndex?: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
};
const deptTableColumns: DeptTableColumn[] = [
  { key: 'name', dataIndex: 'name', title: '部门名称', minWidth: 240 },
  {
    key: 'sortOrder',
    dataIndex: 'sortOrder',
    title: '排序',
    width: 100,
    align: 'center'
  },
  {
    key: 'status',
    dataIndex: 'status',
    title: '状态',
    width: 100,
    align: 'center'
  },
  { key: 'operate', title: '操作', width: 150, align: 'center' }
];
function handleTableExpand(expanded: boolean, row: unknown) {
  const record = row as DeptRow;
  const next = new Set(expandedRowKeys.value);
  if (expanded) next.add(record.id);
  else next.delete(record.id);
  expandedRowKeys.value = [...next];
}
function openEdit(row: Dept) {
  editingId.value = row.id;
  Object.assign(form, {
    parentId: Number(row.parentId) === 0 ? undefined : row.parentId,
    name: row.name,
    sortOrder: row.sortOrder,
    status: row.status
  });
  editorOpen.value = true;
}
function openEditRecord(record: unknown) {
  openEdit(record as Dept);
}
function setParentId(value: string | number | null | undefined) {
  form.parentId = value === null || value === undefined ? undefined : Number(value);
}
async function save() {
  const payload = {
    parentId: form.parentId ?? 0,
    name: form.name.trim(),
    sortOrder: form.sortOrder ?? 0,
    status: form.status
  };
  if (editingId.value) await deptApi.update(editingId.value, payload);
  else await deptApi.create(payload);
  message.success('保存成功');
  editorOpen.value = false;
  await load();
}
async function submitForm() {
  await formRef.value?.validate();
  await save();
}
function remove(row: Dept) {
  Modal.confirm({
    title: `删除部门“${row.name}”？`,
    content: '存在子部门或关联用户时后端会拒绝删除。',
    okType: 'danger',
    async onOk() {
      await deptApi.delete(row.id);
      message.success('删除成功');
      await load();
    }
  });
}
function removeRecord(record: unknown) {
  remove(record as Dept);
}
onMounted(load);
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>部门管理</h1>
        <p>维护组织结构和部门状态</p>
      </div>
      <ASpace wrap>
        <AButton @click="collapseAll">全部收起</AButton>
        <AButton @click="expandAll">全部展开</AButton>
        <AButton @click="load">
          <ReloadOutlined />
          刷新
        </AButton>
        <AButton v-permission="'system:dept:create'" type="primary" @click="openCreate">
          <PlusOutlined />
          新建部门
        </AButton>
      </ASpace>
    </div>
    <div class="query-bar">
      <AInputSearch v-model:value="keyword" allow-clear placeholder="搜索部门名称" />
      <AButton @click="keyword = ''">重置</AButton>
    </div>
    <AlphaTableCard :loading="loading">
      <ATable
        row-key="id"
        :data-source="filteredTreeRows"
        :columns="deptTableColumns"
        :pagination="false"
        :expanded-row-keys="expandedRowKeys"
        :scroll="{ x: 'max-content' }"
        @expand="handleTableExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <ABadge
              :status="record.status === 1 ? 'success' : 'default'"
              :text="record.status === 1 ? '启用' : '停用'"
            />
          </template>
          <template v-else-if="column.key === 'operate'">
            <ASpace :size="4">
              <AButton
                v-permission="'system:dept:create'"
                type="text"
                size="small"
                title="新增子部门"
                @click="openCreateChildRecord(record)"
              >
                <PlusOutlined />
              </AButton>
              <AButton
                v-permission="'system:dept:update'"
                type="text"
                size="small"
                title="编辑部门"
                @click="openEditRecord(record)"
              >
                <EditOutlined />
              </AButton>
              <AButton
                v-permission="'system:dept:delete'"
                type="text"
                danger
                size="small"
                title="删除部门"
                @click="removeRecord(record)"
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
      :title="editingId ? '编辑部门' : '新建部门'"
      ok-text="保存"
      cancel-text="取消"
      @ok="submitForm"
    >
      <AForm ref="formRef" :model="form" :rules="rules" layout="vertical">
        <AFormItem label="部门名称" name="name" required><AInput v-model:value="form.name" /></AFormItem>
        <div class="form-grid">
          <AFormItem label="上级部门" name="parentId">
            <ATreeSelect
              :value="form.parentId"
              class="full-width"
              :tree-data="parentDeptOptions"
              placeholder="请选择上级部门（不选为顶级）"
              :field-names="{
                label: 'title',
                value: 'value',
                children: 'children'
              }"
              allow-clear
              tree-default-expand-all
              @update:value="setParentId"
            />
          </AFormItem>
          <AFormItem label="排序" name="sortOrder">
            <AInputNumber v-model:value="form.sortOrder" class="full-width" />
          </AFormItem>
        </div>
        <AFormItem label="状态">
          <ASwitch v-model:checked="form.status" :checked-value="1" :un-checked-value="0" />
        </AFormItem>
      </AForm>
    </AModal>
  </section>
</template>
