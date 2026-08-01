<script setup lang="ts">
import {
    DeleteOutlined,
    EyeOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'

import AlphaTableCard from '@/components/AlphaTableCard.vue'
import TableActionMenu from '@/components/TableActionMenu.vue'
import {
    redisApi,
    type RedisMetrics,
    type RedisKeyMetadata,
    type RedisOverview,
} from '@/service/redis'

import RedisCommandChart from './redis/RedisCommandChart.vue'
import RedisMemoryGauge from './redis/RedisMemoryGauge.vue'
import RedisTrendChart from './redis/RedisTrendChart.vue'

const prefix = ref('')
const pageSize = ref(50)
const keyword = ref('')
const cursor = ref('0')
const exhausted = ref(false)
const rows = ref<RedisKeyMetadata[]>([])
const overview = ref<RedisOverview>()
const metrics = ref<RedisMetrics>()
const overviewLoading = ref(false)
const metricsLoading = ref(false)
const loading = ref(false)
const overviewError = ref('')
const metricsError = ref('')
const queryError = ref('')
const metadataOpen = ref(false)
const selected = ref<RedisKeyMetadata>()
const deleting = ref<RedisKeyMetadata>()
const deleteOpen = ref(false)
const deleteConfirmation = ref('')

const canDelete = computed(() => deleteConfirmation.value === '删除')
function displayLevelLabel(level: RedisKeyMetadata['displayLevel']) {
    return { HIDDEN: '完全隐藏', MASKED: '已脱敏', PLAIN: '明文' }[level]
}

function displayLevelColor(level: RedisKeyMetadata['displayLevel']) {
    return { HIDDEN: 'red', MASKED: 'orange', PLAIN: 'green' }[level]
}

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
const metricsStatus = computed(() => metrics.value?.status || 'COLLECTING')
const metricsStatusLabel = computed(() => {
    const labels: Record<string, string> = {
        DISABLED: '已关闭',
        COLLECTING: '首次采集中',
        HEALTHY: '采集中',
        DEGRADED: '采样异常',
        STALE: '数据过期',
    }
    return labels[metricsStatus.value] || '状态未知'
})
const metricsStatusColor = computed(() => {
    const colors: Record<string, string> = {
        DISABLED: 'default',
        COLLECTING: 'blue',
        HEALTHY: 'green',
        DEGRADED: 'orange',
        STALE: 'red',
    }
    return colors[metricsStatus.value] || 'default'
})
const metricsStatusDescription = computed(() => {
    if (metricsStatus.value === 'DISABLED') {
        return '增强采样已关闭，概览、受限键查询和单键删除仍可使用。'
    }
    if (metricsStatus.value === 'COLLECTING') {
        return '正在等待 Redis 指标的首次有效采样。'
    }
    if (metricsStatus.value === 'DEGRADED') {
        return '最近采样失败，页面保留最后一次成功数据。'
    }
    if (metricsStatus.value === 'STALE') {
        return '指标超过采样有效期，请检查 Redis 连接或刷新页面。'
    }
    return '每分钟采样当前实例，趋势仅保留在本进程内。'
})
const snapshot = computed(() => metrics.value?.current)
const topCommands = computed(() => metrics.value?.commands || [])
const trends = computed(() => metrics.value?.trend || [])
const redisMetricCards = computed(() => [
    {
        label: '使用内存',
        value: formatBytes(
            snapshot.value?.usedMemoryBytes ?? overview.value?.usedMemoryBytes,
        ),
        hint: 'Redis used_memory',
    },
    {
        label: 'RSS 内存',
        value: formatBytes(snapshot.value?.usedMemoryRssBytes),
        hint: '进程实际驻留内存',
    },
    {
        label: '连接客户端',
        value: formatCount(
            snapshot.value?.connectedClients ??
                overview.value?.connectedClients,
        ),
        hint: '当前连接数',
    },
    {
        label: '每秒命令',
        value: formatDecimal(snapshot.value?.instantaneousOpsPerSecond),
        hint: 'Redis 即时吞吐',
    },
    {
        label: '内存碎片率',
        value: formatDecimal(snapshot.value?.memoryFragmentationRatio),
        hint: 'mem_fragmentation_ratio',
    },
    {
        label: 'Key 数量',
        value: formatCount(discoveredKeyCount.value),
        hint: queryScope.value,
    },
])
const redisBasicInfoRows = computed(() => [
    [
        {
            label: 'Redis 版本',
            value:
                snapshot.value?.redisVersion ||
                overview.value?.redisVersion ||
                '-',
        },
        {
            label: '运行时长',
            value: formatDuration(
                snapshot.value?.uptimeSeconds ?? overview.value?.uptimeSeconds,
            ),
        },
        { label: '采样状态', value: metricsStatusLabel.value },
        {
            label: '采样间隔',
            value: `${formatCount(metrics.value?.sampleIntervalSeconds)} 秒`,
        },
    ],
    [
        {
            label: '使用内存',
            value: formatBytes(
                snapshot.value?.usedMemoryBytes ??
                    overview.value?.usedMemoryBytes,
            ),
        },
        {
            label: 'RSS 内存',
            value: formatBytes(snapshot.value?.usedMemoryRssBytes),
        },
        {
            label: '峰值内存',
            value: formatBytes(snapshot.value?.usedMemoryPeakBytes),
        },
        {
            label: '内存上限',
            value: formatMaxMemory(snapshot.value?.maxMemoryBytes),
        },
    ],
    [
        {
            label: '客户端数',
            value: formatCount(
                snapshot.value?.connectedClients ??
                    overview.value?.connectedClients,
            ),
        },
        {
            label: '阻塞连接',
            value: formatCount(snapshot.value?.blockedClients),
        },
        { label: 'Key 数量', value: formatCount(discoveredKeyCount.value) },
        {
            label: '命令总数',
            value: formatCount(snapshot.value?.totalCommandsProcessed),
        },
    ],
    [
        {
            label: '每秒命令',
            value: formatDecimal(snapshot.value?.instantaneousOpsPerSecond),
        },
        {
            label: '命中/未命中',
            value: `${formatCount(snapshot.value?.keyspaceHits)} / ${formatCount(snapshot.value?.keyspaceMisses)}`,
        },
        {
            label: '最后采样',
            value: formatDateTime(metrics.value?.lastSuccessAt),
        },
        {
            label: '保留窗口',
            value: `${formatCount(metrics.value?.retentionHours)} 小时 / ${formatCount(metrics.value?.maxSamples)} 点`,
        },
    ],
])
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

function toFiniteNumber(value: unknown): number | undefined {
    if (typeof value === 'number') {
        return Number.isFinite(value) ? value : undefined
    }
    if (typeof value !== 'string' || !value.trim()) return undefined
    const numericValue = Number(value)
    return Number.isFinite(numericValue) ? numericValue : undefined
}

function formatCount(value: unknown) {
    const numericValue = toFiniteNumber(value)
    if (numericValue === undefined) return '-'
    return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(
        numericValue,
    )
}

function formatDecimal(value: unknown) {
    const numericValue = toFiniteNumber(value)
    if (numericValue === undefined) return '-'
    return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(numericValue)
}

function formatBytes(value: unknown) {
    const numericValue = toFiniteNumber(value)
    if (numericValue === undefined) return '-'
    if (numericValue === 0) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    const exponent = Math.min(
        Math.floor(Math.log(Math.abs(numericValue)) / Math.log(1024)),
        units.length - 1,
    )
    const scaledValue = numericValue / 1024 ** exponent
    return `${new Intl.NumberFormat('zh-CN', {
        maximumFractionDigits: exponent === 0 ? 0 : 2,
    }).format(scaledValue)} ${units[exponent]}`
}

function formatMaxMemory(value: unknown) {
    const numericValue = toFiniteNumber(value)
    if (numericValue === undefined) return '-'
    if (numericValue === 0) return '未配置'
    return formatBytes(numericValue)
}

function formatDuration(value: unknown) {
    const numericValue = toFiniteNumber(value)
    if (numericValue === undefined) return '-'
    const totalSeconds = Math.max(0, Math.floor(numericValue))
    const days = Math.floor(totalSeconds / 86_400)
    const hours = Math.floor((totalSeconds % 86_400) / 3_600)
    const minutes = Math.floor((totalSeconds % 3_600) / 60)
    if (days > 0) return `${days} 天 ${hours} 小时`
    if (hours > 0) return `${hours} 小时 ${minutes} 分钟`
    return `${minutes} 分钟`
}

function formatDateTime(value: string | undefined) {
    if (!value) return '-'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '-'
    return new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date)
}

async function loadOverview() {
    overviewLoading.value = true
    overviewError.value = ''
    try {
        overview.value = (await redisApi.overview()).data.data
    } catch {
        overviewError.value =
            'Redis 概览加载失败，请确认后端服务和 Redis 连接正常'
        message.error(overviewError.value)
    } finally {
        overviewLoading.value = false
    }
}

async function loadMetrics() {
    metricsLoading.value = true
    metricsError.value = ''
    try {
        metrics.value = (await redisApi.metrics()).data.data
    } catch {
        metricsError.value = 'Redis 指标加载失败，现有概览和键管理仍可使用'
    } finally {
        metricsLoading.value = false
    }
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
    await Promise.all([loadOverview(), loadMetrics(), loadKeys()])
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
            <a-button
                :loading="overviewLoading || metricsLoading || loading"
                @click="refresh"
                ><ReloadOutlined />刷新</a-button
            >
        </div>
        <a-alert
            v-if="overviewError"
            type="error"
            show-icon
            :message="overviewError"
            class="mb-4"
        />
        <a-alert
            v-if="queryError"
            type="error"
            show-icon
            :message="queryError"
            class="mb-4"
        />
        <a-alert
            v-if="metricsError"
            type="warning"
            show-icon
            :message="metricsError"
            class="mb-4"
        />
        <a-tabs class="redis-monitor-tabs" default-active-key="overview">
            <a-tab-pane key="overview" tab="运行概览">
                <section
                    class="redis-panel"
                    aria-labelledby="redis-overview-title"
                >
                    <div class="redis-panel-heading">
                        <div>
                            <h2 id="redis-overview-title">Redis 运行概览</h2>
                            <p>{{ metricsStatusDescription }}</p>
                        </div>
                        <a-tag
                            :color="metricsStatusColor"
                            data-testid="redis-metrics-status"
                        >
                            {{ metricsStatusLabel }}
                        </a-tag>
                    </div>
                    <a-spin :spinning="metricsLoading">
                        <div class="redis-kpi-grid">
                            <div
                                v-for="card in redisMetricCards"
                                :key="card.label"
                                class="redis-kpi-card"
                            >
                                <span
                                    class="redis-kpi-label"
                                    :title="card.label"
                                    >{{ card.label }}</span
                                >
                                <strong
                                    class="redis-kpi-value"
                                    :title="card.value"
                                    >{{ card.value }}</strong
                                >
                                <span
                                    class="redis-kpi-hint"
                                    :title="card.hint"
                                    >{{ card.hint }}</span
                                >
                            </div>
                        </div>
                        <div
                            class="redis-basic-info"
                            aria-label="Redis 基本信息"
                        >
                            <div class="redis-subheading">
                                <h3>Redis 基本信息</h3>
                                <span>仅展示白名单运行字段</span>
                            </div>
                            <div class="redis-basic-info-table">
                                <div
                                    v-for="(
                                        row, rowIndex
                                    ) in redisBasicInfoRows"
                                    :key="rowIndex"
                                    class="redis-basic-info-row"
                                >
                                    <template
                                        v-for="item in row"
                                        :key="item.label"
                                    >
                                        <div
                                            class="redis-basic-info-label"
                                            :title="item.label"
                                        >
                                            {{ item.label }}
                                        </div>
                                        <div
                                            class="redis-basic-info-value"
                                            :title="item.value"
                                        >
                                            {{ item.value }}
                                        </div>
                                    </template>
                                </div>
                            </div>
                        </div>
                        <template v-if="metricsStatus !== 'DISABLED'">
                            <div class="redis-chart-grid">
                                <RedisCommandChart
                                    :commands="topCommands"
                                    :total-calls="
                                        snapshot?.totalCommandsProcessed
                                    "
                                />
                                <RedisMemoryGauge
                                    :used-bytes="snapshot?.usedMemoryBytes"
                                    :max-bytes="snapshot?.maxMemoryBytes"
                                    :total-system-bytes="
                                        snapshot?.totalSystemMemoryBytes
                                    "
                                    :fragmentation-ratio="
                                        snapshot?.memoryFragmentationRatio
                                    "
                                />
                            </div>
                            <div class="redis-command-section">
                                <div class="redis-subheading">
                                    <h3>Top 10 命令</h3>
                                    <span>仅展示安全命令名与统计摘要</span>
                                </div>
                                <AlphaTableCard :loading="metricsLoading">
                                    <a-table
                                        row-key="command"
                                        :data-source="topCommands"
                                        :pagination="false"
                                        :scroll="{ x: 760 }"
                                        size="small"
                                    >
                                        <a-table-column
                                            title="命令"
                                            data-index="command"
                                            width="180"
                                        />
                                        <a-table-column
                                            title="调用次数"
                                            width="130"
                                        >
                                            <template #default="{ record }">
                                                {{ formatCount(record.calls) }}
                                            </template>
                                        </a-table-column>
                                        <a-table-column
                                            title="每秒调用"
                                            width="130"
                                        >
                                            <template #default="{ record }">
                                                {{
                                                    formatDecimal(
                                                        record.callsPerSecond,
                                                    )
                                                }}
                                            </template>
                                        </a-table-column>
                                        <a-table-column
                                            title="失败次数"
                                            width="130"
                                        >
                                            <template #default="{ record }">
                                                {{
                                                    formatCount(
                                                        record.failedCalls,
                                                    )
                                                }}
                                            </template>
                                        </a-table-column>
                                        <a-table-column
                                            title="平均耗时（微秒）"
                                            width="180"
                                        >
                                            <template #default="{ record }">
                                                {{
                                                    formatDecimal(
                                                        record.usecPerCall,
                                                    )
                                                }}
                                            </template>
                                        </a-table-column>
                                    </a-table>
                                </AlphaTableCard>
                            </div>
                        </template>
                        <a-empty
                            v-else
                            description="增强指标已关闭，命令统计和内存图表不可用"
                        />
                    </a-spin>
                </section>
            </a-tab-pane>
            <a-tab-pane key="trends" tab="趋势分析">
                <section
                    class="redis-panel"
                    aria-labelledby="redis-trend-title"
                >
                    <div class="redis-panel-heading">
                        <div>
                            <h2 id="redis-trend-title">Redis 趋势分析</h2>
                            <p>按采样时间展示内存、吞吐与连接变化</p>
                        </div>
                    </div>
                    <a-spin :spinning="metricsLoading">
                        <a-empty
                            v-if="metricsStatus === 'DISABLED'"
                            description="增强指标已关闭，暂无趋势数据"
                        />
                        <div v-else-if="trends.length" class="redis-trend-grid">
                            <RedisTrendChart
                                title="内存趋势"
                                metric="usedMemoryBytes"
                                unit=" 字节"
                                :points="trends"
                                color="#2563eb"
                            />
                            <RedisTrendChart
                                title="命令吞吐趋势"
                                metric="instantaneousOpsPerSecond"
                                unit=" 次/秒"
                                :points="trends"
                                color="#0f766e"
                            />
                            <RedisTrendChart
                                title="连接趋势"
                                metric="connectedClients"
                                unit=" 个"
                                :points="trends"
                                color="#c2410c"
                            />
                        </div>
                        <a-empty
                            v-else
                            description="暂无趋势数据，等待至少一次成功采样"
                        />
                    </a-spin>
                </section>
            </a-tab-pane>
            <a-tab-pane key="keys" tab="键管理">
                <section class="redis-panel" aria-labelledby="redis-keys-title">
                    <div class="redis-panel-heading">
                        <div>
                            <h2 id="redis-keys-title">Redis 键管理</h2>
                            <p>{{ querySummary }}</p>
                        </div>
                    </div>
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
                    <AlphaTableCard :loading="loading">
                        <a-table
                            row-key="key"
                            :data-source="rows"
                            :loading="loading"
                            :pagination="false"
                            :scroll="{ x: 1160 }"
                        >
                            <a-table-column
                                title="键名"
                                data-index="key"
                                width="420"
                            >
                                <template #default="{ text }">
                                    <a-tooltip
                                        :title="text"
                                        overlay-class-name="redis-key-tooltip"
                                    >
                                        <span class="redis-key-cell">{{
                                            text
                                        }}</span>
                                    </a-tooltip>
                                </template>
                            </a-table-column>
                            <a-table-column
                                title="分类"
                                data-index="category"
                                width="160"
                            />
                            <a-table-column
                                title="类型"
                                data-index="type"
                                width="120"
                            />
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
                            <a-table-column
                                title="值预览"
                                data-index="value"
                                width="220"
                            >
                                <template #default="{ record }">
                                    <a-tooltip
                                        :title="
                                            record.displayLevel === 'HIDDEN'
                                                ? undefined
                                                : record.value || ''
                                        "
                                    >
                                        <span class="redis-value-cell">{{
                                            record.value || '-'
                                        }}</span>
                                    </a-tooltip>
                                    <a-tag
                                        class="ml-2"
                                        :color="
                                            displayLevelColor(
                                                record.displayLevel,
                                            )
                                        "
                                    >
                                        {{
                                            displayLevelLabel(
                                                record.displayLevel,
                                            )
                                        }}
                                    </a-tag>
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
                                        <a-menu-item
                                            key="metadata"
                                            @click="inspect(record)"
                                        >
                                            <EyeOutlined />元数据
                                        </a-menu-item>
                                        <a-menu-item
                                            key="delete"
                                            v-permission="
                                                'monitor:redis:delete'
                                            "
                                            data-testid="delete-redis-key"
                                            danger
                                            @click="openDelete(record)"
                                            ><DeleteOutlined />删除</a-menu-item
                                        >
                                    </TableActionMenu>
                                </template>
                            </a-table-column>
                        </a-table>
                    </AlphaTableCard>
                    <div class="redis-result-bar">
                        <span>{{ querySummary }}</span>
                        <a-button
                            :loading="loading"
                            :disabled="!hasMore"
                            @click="loadMore"
                            >加载更多</a-button
                        >
                    </div>
                </section>
            </a-tab-pane>
        </a-tabs>
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
                    <a-tooltip
                        :title="
                            selected.displayLevel === 'HIDDEN'
                                ? undefined
                                : selected.value || ''
                        "
                    >
                        <pre class="redis-value-preview">{{
                            selected.value || '-'
                        }}</pre>
                    </a-tooltip>
                    <a-tag :color="displayLevelColor(selected.displayLevel)">
                        {{ displayLevelLabel(selected.displayLevel) }}
                    </a-tag>
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

<style scoped>
.redis-monitor-tabs {
    margin-top: 8px;
}

.redis-panel {
    margin-bottom: 24px;
    padding: 20px;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: var(--alpha-surface);
    box-shadow: var(--alpha-shadow);
}

.redis-panel-heading,
.redis-subheading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
}

.redis-panel-heading {
    margin-bottom: 18px;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--alpha-border-soft);
}

.redis-panel-heading h2,
.redis-subheading h3 {
    margin: 0;
    color: var(--alpha-text);
    font-size: 18px;
    line-height: 26px;
}

.redis-panel-heading p,
.redis-subheading span {
    margin: 4px 0 0;
    color: var(--alpha-muted);
    font-size: 13px;
    line-height: 20px;
}

.redis-kpi-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
}

.redis-kpi-card {
    min-width: 0;
    padding: 14px;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.redis-kpi-label,
.redis-kpi-hint {
    display: block;
    overflow: hidden;
    color: var(--alpha-muted);
    font-size: 12px;
    line-height: 18px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.redis-kpi-value {
    display: block;
    margin: 6px 0 2px;
    overflow: hidden;
    color: var(--alpha-text);
    font-size: 20px;
    font-variant-numeric: tabular-nums;
    line-height: 28px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.redis-basic-info {
    min-width: 0;
    margin-bottom: 16px;
    padding: 16px;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: #fbfcfe;
}

.redis-basic-info-table {
    overflow-x: auto;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: var(--alpha-surface);
}

.redis-basic-info-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(96px, 0.72fr) minmax(112px, 1fr));
    min-width: 960px;
}

.redis-basic-info-row + .redis-basic-info-row {
    border-top: 1px solid var(--alpha-border-soft);
}

.redis-basic-info-label,
.redis-basic-info-value {
    min-width: 0;
    padding: 11px 12px;
    border-right: 1px solid var(--alpha-border-soft);
    color: var(--alpha-text);
    font-size: 13px;
    line-height: 20px;
    overflow-wrap: anywhere;
}

.redis-basic-info-label {
    background: #f8fafc;
    font-weight: 500;
}

.redis-basic-info-value {
    font-variant-numeric: tabular-nums;
}

.redis-basic-info-row > :last-child {
    border-right: none;
}

.redis-chart-grid {
    display: grid;
    gap: 16px;
    margin-bottom: 16px;
}

.redis-trend-grid {
    display: grid;
    gap: 16px;
}

.redis-command-section {
    margin-top: 20px;
    padding: 16px;
    border: 1px solid var(--alpha-border-soft);
    border-radius: var(--alpha-radius);
    background: #fbfcfe;
}

.redis-subheading {
    align-items: baseline;
    margin-bottom: 10px;
}

.redis-result-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 16px;
    color: var(--alpha-muted);
}

.redis-key-cell,
.redis-value-cell {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: bottom;
    white-space: nowrap;
}

.redis-value-preview {
    max-width: 100%;
    max-height: 360px;
    overflow: auto;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

@media (max-width: 1023px) {
    .redis-kpi-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .redis-chart-grid,
    .redis-trend-grid {
        grid-template-columns: minmax(0, 1fr);
    }
}

@media (max-width: 767px) {
    .redis-panel {
        padding: 14px;
    }

    .redis-kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .redis-kpi-card,
    .redis-basic-info,
    .redis-command-section {
        padding: 12px;
    }

    .redis-panel-heading,
    .redis-subheading,
    .redis-result-bar {
        align-items: flex-start;
        flex-direction: column;
    }

    .redis-basic-info-row {
        grid-template-columns: minmax(96px, 0.7fr) minmax(0, 1fr);
        min-width: 0;
    }

    .redis-basic-info-label:nth-last-child(2),
    .redis-basic-info-value:nth-last-child(1) {
        border-bottom: none;
    }

    .redis-basic-info-label,
    .redis-basic-info-value {
        border-bottom: 1px solid var(--alpha-border-soft);
    }

    .redis-basic-info-value {
        border-right: none;
    }
}
</style>
