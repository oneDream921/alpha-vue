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
import { fileApi, type StoredFile } from '@/service/files'
import { formatDateTime } from '@/utils/dateTime'

const rows = ref<StoredFile[]>([])
const loading = ref(false)
const uploading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
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
            />
            <a-button @click="keyword = ''">重置</a-button>
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
            :scroll="{ x: 920 }"
            @change="changePage"
        >
            <a-table-column title="文件名" data-index="originalName" width="300"
                ><template #default="{ record }"
                    ><div class="file-name-cell">
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
                        <div v-else class="file-type-icon" aria-hidden="true">
                            <FileTextOutlined />
                        </div>
                        <span
                            class="file-name-text"
                            :title="record.originalName"
                            >{{ record.originalName }}</span
                        >
                    </div></template
                ></a-table-column
            >
            <a-table-column title="类型" data-index="contentType" width="170" />
            <a-table-column
                title="大小"
                data-index="sizeBytes"
                width="110"
                align="center"
                ><template #default="{ text }">{{
                    formatSize(text)
                }}</template></a-table-column
            >
            <a-table-column
                title="存储"
                data-index="storageProvider"
                width="100"
                align="center"
                ><template #default="{ text }"
                    ><a-tag>{{ text }}</a-tag></template
                ></a-table-column
            >
            <a-table-column
                title="上传者"
                data-index="uploaderName"
                width="120"
                align="center"
                ><template #default="{ text }">{{
                    text || '-'
                }}</template></a-table-column
            >
            <a-table-column title="上传时间" data-index="createdAt" width="190"
                ><template #default="{ text }">{{
                    formatTime(text)
                }}</template></a-table-column
            >
            <a-table-column title="操作" width="88" align="center"
                ><template #default="{ record }"
                    ><TableActionMenu aria-label="文件操作"
                        ><a-menu-item
                            v-if="record.publicUrl"
                            key="preview"
                            :href="record.publicUrl"
                            target="_blank"
                            rel="noopener"
                            ><EyeOutlined />预览</a-menu-item
                        ><a-menu-item
                            key="delete"
                            v-permission="'file:delete'"
                            danger
                            @click="remove(record)"
                            ><DeleteOutlined />删除</a-menu-item
                        ></TableActionMenu
                    ></template
                ></a-table-column
            >
        </a-table>
    </section>
</template>
