<script setup lang="ts">
/* eslint-disable no-nested-ternary */
import { computed, onMounted, ref } from 'vue';
import { Modal, message } from 'ant-design-vue';
import { DeleteOutlined, EyeOutlined, FileTextOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons-vue';
import { type StoredFile, fileApi } from '@/service/api/files';
import { formatDateTime } from '@/utils/dateTime';
import TableActionMenu from '@/components/TableActionMenu.vue';
import AlphaTableCard from '@/components/AlphaTableCard.vue';
import AlphaTableColumnSetting, { type AlphaTableColumnSettingItem } from '@/components/AlphaTableColumnSetting.vue';
import { useFilePreview } from './useFilePreview';

const rows = ref<StoredFile[]>([]);
const loading = ref(false);
const uploading = ref(false);
const imageUrls = ref<Record<number, string>>({});
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');
const columnSettings = ref<AlphaTableColumnSettingItem[]>([
  { field: 'originalName', title: '文件名', visible: true, align: 'left' },
  { field: 'preview', title: '预览', visible: true, align: 'center' },
  { field: 'contentType', title: '类型', visible: true, align: 'left' },
  { field: 'sizeBytes', title: '大小', visible: true, align: 'center' },
  { field: 'storageProvider', title: '存储', visible: true, align: 'center' },
  { field: 'uploaderName', title: '上传者', visible: true, align: 'center' },
  { field: 'createdAt', title: '上传时间', visible: true, align: 'left' },
  { field: 'operate', title: '操作', visible: true, align: 'center' }
]);
const tableSettingsKey = 'alpha-vue:table:files:v1';
const filteredRows = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  return value
    ? rows.value.filter(item =>
        `${item.originalName} ${item.contentType} ${item.uploaderName ?? ''}`.toLowerCase().includes(value)
      )
    : rows.value;
});
const {
  open: previewOpen,
  loading: previewLoading,
  file: previewFile,
  kind: previewKind,
  text: previewText,
  url: previewUrl,
  error: previewError,
  columns: previewColumns,
  rows: previewRows,
  title: previewTitle,
  preview,
  close: closePreview
} = useFilePreview();
async function load() {
  loading.value = true;
  try {
    const response = await fileApi.page(page.value, size.value);
    rows.value = response.data?.records ?? [];
    total.value = response.data?.total ?? 0;
    const imageRows = rows.value.filter(item => item.contentType.startsWith('image/'));
    const resolved = await Promise.all(
      imageRows.map(async item => {
        try {
          const access = await fileApi.accessUrl(item.id);
          return [item.id, access.data || item.publicUrl] as const;
        } catch {
          return [item.id, item.publicUrl] as const;
        }
      })
    );
    imageUrls.value = Object.fromEntries(resolved);
  } finally {
    loading.value = false;
  }
}
function changePage(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current ?? 1;
  size.value = pagination.pageSize ?? 10;
  load();
}
type FileTableColumn = {
  key: string;
  dataIndex?: string;
  title: string;
  width?: number;
  minWidth?: number;
  align?: 'left' | 'center' | 'right';
};
const fileTableColumns = computed<FileTableColumn[]>(() =>
  columnSettings.value
    .filter(column => column.visible)
    .map(column => ({
      key: column.field,
      dataIndex: column.field === 'operate' ? undefined : column.field,
      title: column.title,
      width:
        column.field === 'preview'
          ? 84
          : column.field === 'sizeBytes'
            ? 110
            : column.field === 'storageProvider'
              ? 100
              : column.field === 'uploaderName'
                ? 120
                : column.field === 'operate'
                  ? 88
                  : undefined,
      minWidth:
        column.field === 'originalName'
          ? 300
          : column.field === 'contentType'
            ? 170
            : column.field === 'createdAt'
              ? 190
              : undefined,
      align: column.align
    }))
);
function fileNameJustify(align?: FileTableColumn['align']) {
  return align === 'center' ? 'center' : align === 'right' ? 'flex-end' : 'flex-start';
}
async function upload(file: File) {
  uploading.value = true;
  try {
    await fileApi.upload(file);
    message.success('上传成功');
    await load();
  } catch {
    message.error('上传失败，请检查文件类型和大小');
  } finally {
    uploading.value = false;
  }
  return false;
}
async function download(row: StoredFile) {
  try {
    const access = await fileApi.accessUrl(row.id);
    const response = await fileApi.content(access.data || row.publicUrl);
    const blob = new Blob([response.data as ArrayBuffer], {
      type: row.contentType || 'application/octet-stream'
    });
    const url = URL.createObjectURL(blob);
    const anchor = globalThis.document.createElement('a');
    anchor.href = url;
    anchor.download = row.originalName;
    globalThis.document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  } catch {
    message.error('下载失败，请稍后重试');
  }
}
function remove(row: StoredFile) {
  Modal.confirm({
    title: `删除文件“${row.originalName}”？`,
    content: '存储对象和文件记录都会被删除。',
    okType: 'danger',
    async onOk() {
      await fileApi.delete(row.id);
      message.success('删除成功');
      await load();
    }
  });
}
function removeRecord(record: unknown) {
  remove(record as StoredFile);
}
function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
const formatTime = formatDateTime;
onMounted(load);
</script>

<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <h1>文件管理</h1>
        <p>查看本地或 MinIO 中的文件元数据</p>
      </div>
      <ASpace wrap>
        <AButton @click="load">
          <ReloadOutlined />
          刷新
        </AButton>
        <AUpload
          v-permission="'file:upload'"
          accept=".png,.jpg,.jpeg,.gif,.webp,.txt,.pdf,.doc,.docx,.xls,.xlsx"
          :show-upload-list="false"
          :before-upload="upload"
        >
          <AButton type="primary" :loading="uploading">
            <UploadOutlined />
            上传文件
          </AButton>
        </AUpload>
      </ASpace>
    </div>
    <div class="query-bar">
      <AInputSearch v-model:value="keyword" allow-clear placeholder="搜索文件名、类型或上传者" />
      <AButton @click="keyword = ''">重置</AButton>
    </div>
    <AlphaTableCard :loading="loading">
      <template #toolbar>
        <AlphaTableColumnSetting v-model="columnSettings" control="align" :storage-key="tableSettingsKey" />
      </template>
      <ATable
        row-key="id"
        :data-source="filteredRows"
        :columns="fileTableColumns"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'originalName'">
            <div class="file-name-cell" :style="{ justifyContent: fileNameJustify(column.align) }">
              <span class="file-name-text" :title="record.originalName">{{ record.originalName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'preview'">
            <AImage
              v-if="record.contentType.startsWith('image/')"
              :src="imageUrls[record.id] || record.publicUrl"
              :width="40"
              :height="40"
              :preview="true"
              class="file-thumbnail"
            />
            <div v-else class="file-type-icon" aria-hidden="true">
              <FileTextOutlined />
            </div>
          </template>
          <template v-else-if="column.key === 'sizeBytes'">
            {{ formatSize(record.sizeBytes) }}
          </template>
          <template v-else-if="column.key === 'storageProvider'">
            <ATag>{{ record.storageProvider }}</ATag>
          </template>
          <template v-else-if="column.key === 'uploaderName'">
            {{ record.uploaderName || '-' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'operate'">
            <TableActionMenu aria-label="文件操作">
              <AMenuItem v-if="record.publicUrl" key="preview" @click="preview(record as StoredFile)">
                <EyeOutlined />
                预览
              </AMenuItem>
              <AMenuItem v-if="record.publicUrl" key="download" @click="download(record as StoredFile)">
                <FileTextOutlined />
                下载
              </AMenuItem>
              <AMenuItem key="delete" v-permission="'file:delete'" danger @click="removeRecord(record)">
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
      :open="previewOpen"
      :title="previewTitle"
      :width="previewKind === 'spreadsheet' ? 1000 : 860"
      :footer="null"
      destroy-on-close
      @cancel="closePreview"
    >
      <ASpin :spinning="previewLoading">
        <div v-if="previewError" class="preview-empty">
          {{ previewError }}
        </div>
        <img
          v-else-if="previewKind === 'image'"
          class="file-preview-image"
          :src="previewUrl"
          :alt="previewFile?.originalName"
        />
        <iframe v-else-if="previewKind === 'pdf'" class="file-preview-pdf" :src="previewUrl" title="PDF 文件预览" />
        <pre v-else-if="previewKind === 'text'" class="file-preview-text">{{ previewText }}</pre>
        <ATable
          v-else-if="previewKind === 'spreadsheet'"
          :columns="previewColumns"
          :data-source="previewRows"
          :pagination="{ pageSize: 20 }"
          :scroll="{ x: 'max-content', y: 520 }"
          size="small"
        />
        <div v-else class="preview-empty">当前文件类型不支持浏览器内预览，请下载后使用本地应用打开。</div>
      </ASpin>
    </AModal>
  </section>
</template>

<style scoped>
.file-name-cell {
  display: flex;
  min-height: 40px;
  align-items: center;
  min-width: 0;
}

.file-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-type-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--alpha-muted);
  font-size: 20px;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

.file-preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
  object-fit: contain;
}

.file-preview-pdf {
  width: 100%;
  height: 70vh;
  border: 0;
}

.file-preview-text {
  max-height: 70vh;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.preview-empty {
  padding: 48px 16px;
  color: var(--alpha-text-secondary);
  text-align: center;
}
</style>
