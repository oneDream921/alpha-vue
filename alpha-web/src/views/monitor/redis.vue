<script setup lang="ts">
import {
    DeleteOutlined,
    EyeOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'

import {
    redisApi,
    type RedisKeyMetadata,
    type RedisOverview,
} from '@/service/redis'

const prefix = ref('auth:')
const count = ref(50)
const cursors = ref(['0'])
const cursorIndex = ref(0)
const rows = ref<RedisKeyMetadata[]>([])
const overview = ref<RedisOverview>()
const loading = ref(false)
const metadataOpen = ref(false)
const selected = ref<RedisKeyMetadata>()
const deleting = ref<RedisKeyMetadata>()
const deleteOpen = ref(false)
const deleteConfirmation = ref('')

const cursor = computed(() => cursors.value[cursorIndex.value])
const canDelete = computed(() => deleteConfirmation.value === '删除')
const prefixOptions = computed(() =>
    Object.keys(
        overview.value?.managedKeyCounts || { 'auth:': 0, 'satoken:': 0 },
    ),
)

async function loadOverview() {
    overview.value = (await redisApi.overview()).data.data
    if (!prefixOptions.value.includes(prefix.value)) {
        prefix.value = prefixOptions.value[0] || ''
    }
}

async function loadKeys() {
    loading.value = true
    try {
        const response = await redisApi.keys({
            prefix: prefix.value,
            cursor: cursor.value,
            count: count.value,
        })
        rows.value = response.data.data.records
        const next = response.data.data.nextCursor
        if (
            response.data.data.hasMore &&
            cursors.value[cursorIndex.value + 1] !== next
        ) {
            cursors.value = [
                ...cursors.value.slice(0, cursorIndex.value + 1),
                next,
            ]
        }
    } finally {
        loading.value = false
    }
}

function changePrefix() {
    cursors.value = ['0']
    cursorIndex.value = 0
    void loadKeys()
}

function nextPage() {
    if (cursors.value[cursorIndex.value + 1]) {
        cursorIndex.value += 1
        void loadKeys()
    }
}

function previousPage() {
    if (cursorIndex.value > 0) {
        cursorIndex.value -= 1
        void loadKeys()
    }
}

async function inspect(row: RedisKeyMetadata) {
    selected.value = (await redisApi.key(row.key)).data.data
    metadataOpen.value = true
}

function openDelete(row: RedisKeyMetadata) {
    deleting.value = row
    deleteConfirmation.value = ''
    deleteOpen.value = true
}

async function confirmDelete() {
    if (!deleting.value || !canDelete.value) return
    await redisApi.delete(deleting.value.key)
    message.success('Redis 键已删除；相关会话或验证码将立即失效')
    deleting.value = undefined
    deleteOpen.value = false
    await loadKeys()
    await loadOverview()
}

async function refresh() {
    await loadOverview()
    await loadKeys()
}

onMounted(refresh)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>Redis 管理</h1>
                <p>仅管理 Alpha Vue 受控键前缀；验证码和会话内容始终脱敏</p>
            </div>
            <a-button :loading="loading" @click="refresh"
                ><ReloadOutlined />刷新</a-button
            >
        </div>
        <a-alert
            type="warning"
            show-icon
            message="验证码内容已脱敏；删除会话键将使对应用户下线。此处不支持全库扫描、清空或批量删除。"
            class="mb-4"
        />
        <a-row :gutter="16" class="mb-4">
            <a-col :xs="12" :md="6"
                ><a-statistic
                    title="Redis 版本"
                    :value="overview?.redisVersion || '-'"
            /></a-col>
            <a-col :xs="12" :md="6"
                ><a-statistic
                    title="已用内存（字节）"
                    :value="overview?.usedMemoryBytes || 0"
            /></a-col>
            <a-col :xs="12" :md="6"
                ><a-statistic
                    title="连接客户端"
                    :value="overview?.connectedClients || 0"
            /></a-col>
            <a-col :xs="12" :md="6"
                ><a-statistic
                    title="运行秒数"
                    :value="overview?.uptimeSeconds || 0"
            /></a-col>
        </a-row>
        <div class="query-bar">
            <a-select
                v-model:value="prefix"
                :options="
                    prefixOptions.map((value) => ({ value, label: value }))
                "
                @change="changePrefix"
            />
            <a-select
                v-model:value="count"
                :options="
                    [20, 50, 100].map((value) => ({
                        value,
                        label: `${value} 条`,
                    }))
                "
                @change="changePrefix"
            />
        </div>
        <a-table
            row-key="key"
            :data-source="rows"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 900 }"
        >
            <a-table-column title="键名" data-index="key" width="300" />
            <a-table-column title="分类" data-index="category" width="160" />
            <a-table-column title="类型" data-index="type" width="120" />
            <a-table-column
                title="TTL（秒）"
                data-index="ttlSeconds"
                width="130"
            />
            <a-table-column
                title="大小估计（字节）"
                data-index="sizeBytes"
                width="170"
            />
            <a-table-column title="操作" width="180" fixed="right">
                <template #default="{ record }">
                    <a-space>
                        <a-button
                            type="link"
                            size="small"
                            @click="inspect(record)"
                            ><EyeOutlined />元数据</a-button
                        >
                        <a-button
                            v-permission="'monitor:redis:delete'"
                            data-testid="delete-redis-key"
                            type="link"
                            danger
                            size="small"
                            @click="openDelete(record)"
                            ><DeleteOutlined />删除</a-button
                        >
                    </a-space>
                </template>
            </a-table-column>
        </a-table>
        <a-space class="mt-4">
            <a-button :disabled="cursorIndex === 0" @click="previousPage"
                >上一页</a-button
            >
            <span>游标第 {{ cursorIndex + 1 }} 页</span>
            <a-button :disabled="!cursors[cursorIndex + 1]" @click="nextPage"
                >下一页</a-button
            >
        </a-space>
        <a-drawer
            v-model:open="metadataOpen"
            title="Redis 键元数据"
            placement="right"
        >
            <a-descriptions v-if="selected" :column="1" bordered>
                <a-descriptions-item label="键名">{{
                    selected.key
                }}</a-descriptions-item>
                <a-descriptions-item label="分类">{{
                    selected.category
                }}</a-descriptions-item>
                <a-descriptions-item label="类型">{{
                    selected.type
                }}</a-descriptions-item>
                <a-descriptions-item label="TTL">{{
                    selected.ttlSeconds
                }}</a-descriptions-item>
                <a-descriptions-item label="值"
                    >已脱敏，不可读取</a-descriptions-item
                >
            </a-descriptions>
        </a-drawer>
        <a-modal
            v-model:open="deleteOpen"
            title="确认删除 Redis 键"
            ok-text="删除"
            ok-type="danger"
            :ok-button-props="{ disabled: !canDelete }"
            @ok="confirmDelete"
        >
            <p>
                删除验证码会使其失效；删除会话键会使对应用户下线。键名不会写入审计日志。
            </p>
            <p>请输入“删除”确认：</p>
            <a-input v-model:value="deleteConfirmation" autocomplete="off" />
        </a-modal>
    </section>
</template>
