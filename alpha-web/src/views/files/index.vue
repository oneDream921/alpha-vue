<script setup lang="ts">
import {
    DeleteOutlined,
    EyeOutlined,
    FileTextOutlined,
    ReloadOutlined,
    UploadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'

import TableActionMenu from '@/components/TableActionMenu.vue'
import AlphaTableCard from '@/components/AlphaTableCard.vue'
import TableColumnSetting, {
    type TableColumnSettingItem,
} from '@/components/TableColumnSetting.vue'
import { fileApi, type StoredFile } from '@/service/files/index'
import { formatDateTime } from '@/utils/dateTime'
import { useFilePreview } from './useFilePreview'

const rows = ref<StoredFile[]>([])
const loading = ref(false)
const uploading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const columnSettings = ref<TableColumnSettingItem[]>([
    { field: 'originalName', title: '文件名', visible: true, align: 'left' },
    { field: 'contentType', title: '类型', visible: true, align: 'left' },
    { field: 'sizeBytes', title: '大小', visible: true, align: 'center' },
    { field: 'storageProvider', title: '存储', visible: true, align: 'center' },
    { field: 'uploaderName', title: '上传者', visible: true, align: 'center' },
    { field: 'createdAt', title: '上传时间', visible: true, align: 'left' },
    { field: 'operate', title: '操作', visible: true, align: 'center' },
])
const tableSettingsKey = 'alpha-vue:table:files:v1'
const filteredRows = computed(() => {
    const value = keyword.value.trim().toLowerCase()
    return value
        ? rows.value.filter((item) =>
              `${item.originalName} ${item.contentType} ${item.uploaderName ?? ''}`
                  .toLowerCase()
                  .includes(value),
          )
        : rows.value
})
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
    close: closePreview,
} = useFilePreview()
async function load() {
    loading.value = true
    try {
        const response = await fileApi.page(page.value, size.value)
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
type FileTableColumn = {
    key: string
    dataIndex?: string
    title: string
    width?: number
    minWidth?: number
    align?: 'left' | 'center' | 'right'
}
const fileTableColumns = computed<FileTableColumn[]>(() =>
    columnSettings.value
        .filter((column) => column.visible)
        .map((column) => ({
            key: column.field,
            dataIndex: column.field === 'operate' ? undefined : column.field,
            title: column.title,
            width:
                column.field === 'sizeBytes'
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
            align: column.align,
        })),
)
function fileNameJustify(align?: FileTableColumn['align']) {
    return align === 'center'
        ? 'center'
        : align === 'right'
          ? 'flex-end'
          : 'flex-start'
}
async function upload(file: File) {
    uploading.value = true
    try {
        await fileApi.upload(file)
        message.success('上传成功')
        await load()
    } catch {
        message.error('上传失败，请检查文件类型和大小')
    } finally {
        uploading.value = false
    }
    return false
}
async function download(row: StoredFile) {
    try {
        const response = await fileApi.content(row.publicUrl)
        const blob = new Blob([response.data as ArrayBuffer], {
            type: row.contentType || 'application/octet-stream',
        })
        const url = URL.createObjectURL(blob)
        const anchor = globalThis.document.createElement('a')
        anchor.href = url
        anchor.download = row.originalName
        globalThis.document.body.appendChild(anchor)
        anchor.click()
        anchor.remove()
        window.setTimeout(() => URL.revokeObjectURL(url), 0)
    } catch {
        message.error('下载失败，请稍后重试')
    }
}
function remove(row: StoredFile) {
    Modal.confirm({
        title: `删除文件“${row.originalName}”？`,
        content: '存储对象和文件记录都会被删除。',
        okType: 'danger',
        async onOk() {
            await fileApi.delete(row.id)
            message.success('删除成功')
            await load()
        },
    })
}
function removeRecord(record: unknown) {
    remove(record as StoredFile)
}
function formatSize(bytes: number) {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
const formatTime = formatDateTime
onMounted(load)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>文件管理</h1>
                <p>查看本地或 MinIO 中的文件元数据</p>
            </div>
            <a-space wrap>
                <a-button @click="load"><ReloadOutlined />刷新</a-button>
                <a-upload
                    v-permission="'file:upload'"
                    accept=".png,.jpg,.jpeg,.gif,.webp,.txt,.pdf,.doc,.docx,.xls,.xlsx"
                    :show-upload-list="false"
                    :before-upload="upload"
                    ><a-button type="primary" :loading="uploading"
                        ><UploadOutlined />上传文件</a-button
                    ></a-upload
                >
            </a-space>
        </div>
        <div class="query-bar">
            <a-input-search
                v-model:value="keyword"
                allow-clear
                placeholder="搜索文件名、类型或上传者"
            /><a-button @click="keyword = ''">重置</a-button>
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
                :columns="fileTableColumns"
                :pagination="false"
                :scroll="{ x: 'max-content' }"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'originalName'">
                        <div
                            class="file-name-cell"
                            :style="{
                                justifyContent: fileNameJustify(column.align),
                            }"
                        >
                            <a-image
                                v-if="
                                    record.publicUrl &&
                                    record.contentType.startsWith('image/')
                                "
                                :src="record.publicUrl"
                                :width="40"
                                :height="40"
                                :preview="true"
                                class="file-thumbnail"
                            />
                            <div
                                v-else
                                class="file-type-icon"
                                aria-hidden="true"
                            >
                                <FileTextOutlined />
                            </div>
                            <span
                                class="file-name-text"
                                :title="record.originalName"
                                >{{ record.originalName }}</span
                            >
                        </div>
                    </template>
                    <template v-else-if="column.key === 'sizeBytes'">
                        {{ formatSize(record.sizeBytes) }}
                    </template>
                    <template v-else-if="column.key === 'storageProvider'">
                        <a-tag>{{ record.storageProvider }}</a-tag>
                    </template>
                    <template v-else-if="column.key === 'uploaderName'">
                        {{ record.uploaderName || '-' }}
                    </template>
                    <template v-else-if="column.key === 'createdAt'">
                        {{ formatTime(record.createdAt) }}
                    </template>
                    <template v-else-if="column.key === 'operate'">
                        <TableActionMenu aria-label="文件操作">
                            <a-menu-item
                                v-if="record.publicUrl"
                                key="preview"
                                @click="preview(record as StoredFile)"
                                ><EyeOutlined />预览</a-menu-item
                            ><a-menu-item
                                v-if="record.publicUrl"
                                key="download"
                                @click="download(record as StoredFile)"
                                ><FileTextOutlined />下载</a-menu-item
                            ><a-menu-item
                                key="delete"
                                v-permission="'file:delete'"
                                danger
                                @click="removeRecord(record)"
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
                        (current, pageSize) => changePage({ current, pageSize })
                    "
                />
            </template>
        </AlphaTableCard>
        <a-modal
            :open="previewOpen"
            :title="previewTitle"
            :width="previewKind === 'spreadsheet' ? 1000 : 860"
            :footer="null"
            destroy-on-close
            @cancel="closePreview"
        >
            <a-spin :spinning="previewLoading">
                <div v-if="previewError" class="preview-empty">
                    {{ previewError }}
                </div>
                <img
                    v-else-if="previewKind === 'image'"
                    class="file-preview-image"
                    :src="previewUrl"
                    :alt="previewFile?.originalName"
                />
                <iframe
                    v-else-if="previewKind === 'pdf'"
                    class="file-preview-pdf"
                    :src="previewUrl"
                    title="PDF 文件预览"
                />
                <pre
                    v-else-if="previewKind === 'text'"
                    class="file-preview-text"
                    >{{ previewText }}</pre>
                <a-table
                    v-else-if="previewKind === 'spreadsheet'"
                    :columns="previewColumns"
                    :data-source="previewRows"
                    :pagination="{ pageSize: 20 }"
                    :scroll="{ x: 'max-content', y: 520 }"
                    size="small"
                />
                <div v-else class="preview-empty">
                    当前文件类型不支持浏览器内预览，请下载后使用本地应用打开。
                </div>
            </a-spin>
        </a-modal>
    </section>
</template>

<style scoped>
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
