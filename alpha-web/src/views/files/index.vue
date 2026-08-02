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
import * as XLSX from 'xlsx'

import TableActionMenu from '@/components/TableActionMenu.vue'
import AlphaTableCard from '@/components/AlphaTableCard.vue'
import TableColumnSetting, {
    type TableColumnSettingItem,
} from '@/components/TableColumnSetting.vue'
import { fileApi, type StoredFile } from '@/service/files'
import { formatDateTime } from '@/utils/dateTime'

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
type PreviewKind = 'image' | 'pdf' | 'text' | 'spreadsheet' | 'unsupported'
type PreviewColumn = {
    title: string
    dataIndex: string
    key: string
}
const previewOpen = ref(false)
const previewLoading = ref(false)
const previewFile = ref<StoredFile | null>(null)
const previewKind = ref<PreviewKind>('unsupported')
const previewText = ref('')
const previewUrl = ref('')
const previewError = ref('')
const previewColumns = ref<PreviewColumn[]>([])
const previewRows = ref<Record<string, string>[]>([])

function isContentType(file: StoredFile, type: string) {
    return file.contentType.toLowerCase() === type
}

function isSpreadsheet(file: StoredFile) {
    return (
        isContentType(file, 'application/vnd.ms-excel') ||
        isContentType(
            file,
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        )
    )
}

function isText(file: StoredFile) {
    return isContentType(file, 'text/plain')
}

function clearPreviewUrl() {
    if (previewUrl.value.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = ''
}

function closePreview() {
    previewOpen.value = false
    clearPreviewUrl()
    previewFile.value = null
    previewText.value = ''
    previewError.value = ''
    previewColumns.value = []
    previewRows.value = []
}

function spreadsheetRows(data: unknown[][]) {
    const source = data.slice(0, 200)
    const width = Math.min(
        50,
        source.reduce((maximum, row) => Math.max(maximum, row.length), 0),
    )
    const columns = Array.from({ length: width }, (_, index) => ({
        title: String(source[0]?.[index] || `列 ${index + 1}`),
        dataIndex: `column_${index}`,
        key: `column_${index}`,
    }))
    const rows = source
        .slice(1)
        .map((row) =>
            Object.fromEntries(
                columns.map((column, columnIndex) => [
                    column.dataIndex,
                    String(row[columnIndex] ?? ''),
                ]),
            ),
        )
    return {
        columns,
        rows: rows.map((row, index) => ({ ...row, key: `${index}` })),
    }
}

async function preview(row: StoredFile) {
    closePreview()
    previewFile.value = row
    previewOpen.value = true
    previewLoading.value = true
    previewKind.value = row.contentType.startsWith('image/')
        ? 'image'
        : isContentType(row, 'application/pdf')
          ? 'pdf'
          : isText(row)
            ? 'text'
            : isSpreadsheet(row)
              ? 'spreadsheet'
              : 'unsupported'
    if (previewKind.value === 'image') {
        previewUrl.value = row.publicUrl
        previewLoading.value = false
        return
    }
    if (previewKind.value === 'unsupported') {
        previewLoading.value = false
        return
    }
    try {
        const response = await fileApi.content(
            row.publicUrl,
            previewKind.value === 'text' ? 'text' : 'arraybuffer',
        )
        if (previewKind.value === 'pdf') {
            const blob = new Blob([response.data as ArrayBuffer], {
                type: row.contentType,
            })
            previewUrl.value = URL.createObjectURL(blob)
        } else if (previewKind.value === 'text') {
            previewText.value = response.data as string
        } else {
            const workbook = XLSX.read(response.data as ArrayBuffer, {
                type: 'array',
                cellDates: true,
            })
            const firstSheet = workbook.Sheets[workbook.SheetNames[0] ?? '']
            if (!firstSheet) {
                throw new Error('工作簿没有可预览的工作表')
            }
            const data = XLSX.utils.sheet_to_json<unknown[]>(firstSheet, {
                header: 1,
                raw: false,
                defval: '',
            })
            const parsed = spreadsheetRows(data)
            previewColumns.value = parsed.columns
            previewRows.value = parsed.rows
        }
    } catch {
        previewError.value = '文件内容读取失败，请下载文件后查看。'
    } finally {
        previewLoading.value = false
    }
}
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
                                :href="record.publicUrl"
                                target="_blank"
                                rel="noopener"
                                download
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
            :title="
                previewFile ? `预览：${previewFile.originalName}` : '文件预览'
            "
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
