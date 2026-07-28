<script setup lang="ts">
import {
    DeleteOutlined,
    EyeOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'

import TableActionMenu from '@/components/TableActionMenu.vue'
import {
    redisApi,
    type RedisKeyMetadata,
    type RedisOverview,
} from '@/service/redis'

const prefix = ref('')
const pageSize = ref(50)
const keyword = ref('')
const cursor = ref('0')
const exhausted = ref(false)
const rows = ref<RedisKeyMetadata[]>([])
const overview = ref<RedisOverview>()
const loading = ref(false)
const queryError = ref('')
const metadataOpen = ref(false)
const selected = ref<RedisKeyMetadata>()
const deleting = ref<RedisKeyMetadata>()
const deleteOpen = ref(false)
const deleteConfirmation = ref('')

const canDelete = computed(() => deleteConfirmation.value === '删除')
const discoveredKeyCount = computed(
    () => overview.value?.managedKeyCounts?.['全部 Redis 键'] || 0,
)
const hasMore = computed(() => !queryError.value && !exhausted.value)
const queryScope = computed(() =>
    prefix.value.trim() ? `前缀 ${prefix.value.trim()}*` : '全库',
)
const querySummary = computed(() => {
    const discovered = prefix.value.trim()
        ? ''
        : `，概览已发现 ${discoveredKeyCount.value} 个键`
    return `已显示 ${rows.value.length} 条 / ${queryScope.value}${discovered}`
})
const deletingImpact = computed(() => {
    const row = deleting.value
    if (!row) return ''
    if (row.category === '验证码') {
        return '删除后对应验证码会立即失效，用户需要重新获取验证码。'
    }
    if (row.category === '登录失败窗口') {
        return '删除后会清除该账号/IP 的登录失败计数，可能解除当前登录限制。'
    }
    if (row.category === 'Sa-Token 会话') {
        return '删除后对应登录会话会立即失效，相关用户会被下线。'
    }
    return '删除后可能影响业务缓存、临时状态、队列进度或第三方集成，请确认该键可以安全移除。'
})

async function loadOverview() {
    overview.value = (await redisApi.overview()).data.data
}

function resetScanState() {
    rows.value = []
    cursor.value = '0'
    exhausted.value = false
}

async function loadKeys(append = false) {
    if (!append) {
        resetScanState()
    }
    queryError.value = ''
    loading.value = true
    try {
        const search = keyword.value.trim()
        const response = await redisApi.keys({
            prefix: prefix.value.trim(),
            cursor: cursor.value,
            count: pageSize.value,
            keyword: search || undefined,
        })
        const page = response.data.data
        cursor.value = page.nextCursor
        exhausted.value = !page.hasMore
        rows.value = append ? [...rows.value, ...page.records] : page.records
    } catch {
        queryError.value =
            'Redis 键列表加载失败，请确认后端服务和 Redis 连接正常'
        if (!append) {
            rows.value = []
        }
        exhausted.value = true
        message.error(queryError.value)
    } finally {
        loading.value = false
    }
}

function changeQuery() {
    void loadKeys()
}

function loadMore() {
    if (hasMore.value) {
        void loadKeys(true)
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
    await loadOverview()
    await loadKeys()
}

async function refresh() {
    queryError.value = ''
    loading.value = true
    try {
        await loadOverview()
    } catch {
        queryError.value = 'Redis 概览加载失败，请确认后端服务和 Redis 连接正常'
        rows.value = []
        exhausted.value = true
        message.error(queryError.value)
        loading.value = false
        return
    }
    loading.value = false
    await loadKeys()
}

onMounted(refresh)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>Redis 管理</h1>
                <p>可查询全库键和值内容；删除前请确认影响</p>
            </div>
            <a-button @click="refresh"><ReloadOutlined />刷新</a-button>
        </div>
        <a-alert
            v-if="queryError"
            type="error"
            show-icon
            :message="queryError"
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
        <div class="query-bar redis-query-bar">
            <a-input-search
                v-model:value="prefix"
                class="redis-prefix-search"
                placeholder="前缀筛选（可选，例如 satoken:）"
                enter-button="筛选"
                allow-clear
                @search="changeQuery"
            />
            <a-input-search
                v-model:value="keyword"
                class="redis-keyword-search"
                placeholder="键名关键词"
                enter-button="查询"
                allow-clear
                @search="changeQuery"
            />
            <a-select
                v-model:value="pageSize"
                class="redis-page-size-select"
                :options="
                    [20, 50, 100].map((value) => ({
                        value,
                        label: `每次加载 ${value} 条`,
                    }))
                "
                :dropdown-match-select-width="false"
                popup-class-name="redis-select-popup"
                @change="changeQuery"
            />
        </div>
        <a-table
            row-key="key"
            :data-source="rows"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1160 }"
        >
            <a-table-column title="键名" data-index="key" width="420">
                <template #default="{ text }">
                    <a-tooltip
                        :title="text"
                        overlay-class-name="redis-key-tooltip"
                    >
                        <span class="redis-key-cell">{{ text }}</span>
                    </a-tooltip>
                </template>
            </a-table-column>
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
            <a-table-column title="值预览" data-index="value" width="220">
                <template #default="{ record }">
                    <span
                        class="redis-value-cell"
                        :title="record.value || ''"
                        >{{ record.value || '-' }}</span
                    >
                    <a-tag v-if="record.valueTruncated" class="ml-2"
                        >已截断</a-tag
                    >
                </template>
            </a-table-column>
            <a-table-column
                title="操作"
                width="88"
                fixed="right"
                align="center"
            >
                <template #default="{ record }">
                    <TableActionMenu aria-label="Redis 键操作">
                        <a-menu-item key="metadata" @click="inspect(record)">
                            <EyeOutlined />元数据
                        </a-menu-item>
                        <a-menu-item
                            key="delete"
                            v-permission="'monitor:redis:delete'"
                            data-testid="delete-redis-key"
                            danger
                            @click="openDelete(record)"
                            ><DeleteOutlined />删除</a-menu-item
                        >
                    </TableActionMenu>
                </template>
            </a-table-column>
        </a-table>
        <div class="redis-result-bar">
            <span>{{ querySummary }}</span>
            <a-button :loading="loading" :disabled="!hasMore" @click="loadMore"
                >加载更多</a-button
            >
        </div>
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
                <a-descriptions-item label="值">
                    <pre class="redis-value-preview">{{
                        selected.value || '-'
                    }}</pre>
                    <a-tag v-if="selected.valueTruncated">已截断</a-tag>
                </a-descriptions-item>
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
                {{ deletingImpact }}
            </p>
            <p>请输入“删除”确认：</p>
            <a-input v-model:value="deleteConfirmation" autocomplete="off" />
        </a-modal>
    </section>
</template>
