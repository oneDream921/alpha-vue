<script setup lang="ts">
import {
    ClearOutlined,
    CopyOutlined,
    LinkOutlined,
    PauseCircleOutlined,
    ReloadOutlined,
    SettingOutlined,
    PlayCircleOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { computed, onMounted, ref } from 'vue'

import {
    sqlMonitorApi,
    type DruidInfo,
    type SqlLogEntry,
    type SqlLogSettings,
    type SqlLogStatement,
} from '@/service/sqlMonitor'

const rows = ref<SqlLogEntry[]>([])
const druid = ref<DruidInfo>()
const loading = ref(false)
const clearing = ref(false)
const queryError = ref('')
const sqlDetailOpen = ref(false)
const mapperDrawerOpen = ref(false)
const selectedSql = ref<SqlLogEntry>()
const settings = ref<SqlLogSettings>({
    enabled: true,
    statements: [],
    excludedStatementIds: [],
})
const settingsSaving = ref(false)
const limit = ref(100)
const sqlType = ref('ALL')
const keyword = ref('')
const slowOnly = ref(false)

const typeOptions = [
    { value: 'ALL', label: '全部类型' },
    { value: 'SELECT', label: 'SELECT' },
    { value: 'INSERT', label: 'INSERT' },
    { value: 'UPDATE', label: 'UPDATE' },
    { value: 'DELETE', label: 'DELETE' },
    { value: 'UNKNOWN', label: 'UNKNOWN' },
]

const slowCount = computed(() => rows.value.filter((row) => row.slow).length)
const enabledLabel = computed(() =>
    settings.value.enabled ? '采集中' : '已暂停',
)
const enabledColor = computed(() =>
    settings.value.enabled ? 'green' : 'orange',
)
const druidPath = computed(() => druid.value?.path || '/druid/index.html')
const druidDisabled = computed(() => !druid.value?.enabled)
const druidUrl = computed(() => resolveBackendUrl(druidPath.value))
const excludedStatementIds = computed(
    () => new Set(settings.value.excludedStatementIds),
)
const checkedStatementIds = computed(() =>
    settings.value.statements
        .map((statement) => statement.statementId)
        .filter((statementId) => !excludedStatementIds.value.has(statementId)),
)
const mapperTreeData = computed(() =>
    buildMapperTree(settings.value.statements),
)

function formatTime(value: string) {
    return new Intl.DateTimeFormat('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
    }).format(new Date(value))
}

async function loadDruidInfo() {
    druid.value = (await sqlMonitorApi.druidUrl()).data.data
}

async function loadSettings() {
    settings.value = (await sqlMonitorApi.settings()).data.data
}

async function loadLogs() {
    queryError.value = ''
    loading.value = true
    try {
        const search = keyword.value.trim()
        rows.value = (
            await sqlMonitorApi.logs({
                limit: limit.value,
                type: sqlType.value === 'ALL' ? undefined : sqlType.value,
                keyword: search || undefined,
                slowOnly: slowOnly.value,
            })
        ).data.data
    } catch {
        queryError.value = 'SQL 日志加载失败，请确认后端服务正常'
        message.error(queryError.value)
    } finally {
        loading.value = false
    }
}

async function refresh() {
    await Promise.all([loadDruidInfo(), loadSettings(), loadLogs()])
}

function openDruid() {
    window.open(druidUrl.value, '_blank', 'noopener,noreferrer')
}

function resolveBackendUrl(path: string) {
    if (/^https?:\/\//i.test(path)) {
        return path
    }
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    const backendOrigin =
        window.location.port === '5173'
            ? 'http://localhost:8080'
            : window.location.origin
    return `${backendOrigin}${normalizedPath}`
}

async function copySql(row: SqlLogEntry) {
    await window.navigator.clipboard.writeText(row.sql)
    message.success('SQL 已复制')
}

function openSqlDetail(row: SqlLogEntry) {
    selectedSql.value = row
    sqlDetailOpen.value = true
}

async function updateCollectionEnabled(enabled: boolean) {
    await saveSettings(enabled, settings.value.excludedStatementIds)
    message.success(enabled ? 'SQL 日志采集已开始' : 'SQL 日志采集已暂停')
}

async function updateCheckedStatements(checkedKeys: unknown) {
    const keys = checkedKeyArray(checkedKeys)
    const checked = new Set(keys.map(String))
    const excluded = settings.value.statements
        .map((statement) => statement.statementId)
        .filter((statementId) => !checked.has(statementId))
    await saveSettings(settings.value.enabled, excluded)
    message.success('Mapper 采集范围已更新')
}

async function saveSettings(enabled: boolean, excludedStatementIds: string[]) {
    settingsSaving.value = true
    try {
        settings.value = (
            await sqlMonitorApi.updateSettings({
                enabled,
                excludedStatementIds,
            })
        ).data.data
    } finally {
        settingsSaving.value = false
    }
}

function clearLogs() {
    Modal.confirm({
        title: '清空 SQL 日志',
        content: '将清空当前内存中的最近 SQL 摘要，不影响数据库和审计日志。',
        okText: '清空',
        okType: 'danger',
        cancelText: '取消',
        async onOk() {
            clearing.value = true
            try {
                await sqlMonitorApi.clear()
                rows.value = []
                message.success('SQL 日志已清空')
            } finally {
                clearing.value = false
            }
        },
    })
}

onMounted(refresh)

function buildMapperTree(statements: SqlLogStatement[]) {
    const mapperGroups = new Map<string, SqlLogStatement[]>()
    statements.forEach((statement) => {
        const group = mapperGroups.get(statement.mapperName) || []
        group.push(statement)
        mapperGroups.set(statement.mapperName, group)
    })
    return [...mapperGroups.entries()].map(([mapperName, items]) => ({
        title: shortMapperName(mapperName),
        key: mapperName,
        children: items.map((item) => ({
            title: item.methodName,
            key: item.statementId,
        })),
    }))
}

function shortMapperName(mapperName: string) {
    const parts = mapperName.split('.')
    return parts.slice(-2).join('.')
}

function checkedKeyArray(value: unknown) {
    if (Array.isArray(value)) {
        return value
    }
    if (
        value &&
        typeof value === 'object' &&
        'checked' in value &&
        Array.isArray(value.checked)
    ) {
        return value.checked
    }
    return []
}
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>SQL 日志</h1>
                <p>
                    最近 {{ rows.length }} 条，慢 SQL {{ slowCount }} 条
                    <a-tag :color="enabledColor">{{ enabledLabel }}</a-tag>
                </p>
            </div>
            <a-space wrap>
                <a-button :disabled="druidDisabled" @click="openDruid">
                    <LinkOutlined />Druid
                </a-button>
                <a-button
                    v-permission="'monitor:sql:control'"
                    :loading="settingsSaving"
                    @click="updateCollectionEnabled(!settings.enabled)"
                >
                    <PlayCircleOutlined v-if="!settings.enabled" />
                    <PauseCircleOutlined v-else />{{
                        settings.enabled ? '暂停采集' : '开始采集'
                    }}
                </a-button>
                <a-button
                    v-permission="'monitor:sql:control'"
                    @click="mapperDrawerOpen = true"
                    ><SettingOutlined />Mapper</a-button
                >
                <a-button @click="refresh"><ReloadOutlined />刷新</a-button>
                <a-button
                    v-permission="'monitor:sql:clear'"
                    danger
                    :loading="clearing"
                    @click="clearLogs"
                    ><ClearOutlined />清空</a-button
                >
            </a-space>
        </div>
        <a-alert
            v-if="queryError"
            type="error"
            show-icon
            :message="queryError"
            class="mb-4"
        />
        <div class="query-bar sql-query-bar">
            <a-select
                v-model:value="sqlType"
                class="sql-type-select"
                :options="typeOptions"
                :dropdown-match-select-width="false"
                @change="loadLogs"
            />
            <a-select
                v-model:value="limit"
                class="sql-limit-select"
                :options="
                    [50, 100, 200].map((value) => ({
                        value,
                        label: `最近 ${value} 条`,
                    }))
                "
                :dropdown-match-select-width="false"
                @change="loadLogs"
            />
            <a-checkbox v-model:checked="slowOnly" @change="loadLogs"
                >只看慢 SQL</a-checkbox
            >
            <a-input-search
                v-model:value="keyword"
                class="sql-keyword-search"
                placeholder="SQL / 表名 / traceId"
                enter-button="查询"
                allow-clear
                @search="loadLogs"
            />
        </div>
        <div class="sql-table-wrap">
            <a-table
                row-key="id"
                :data-source="rows"
                :loading="loading"
                :pagination="false"
                :scroll="{ x: 1540 }"
            >
                <a-table-column title="时间" data-index="createdAt" width="110">
                    <template #default="{ text }">{{
                        formatTime(text)
                    }}</template>
                </a-table-column>
                <a-table-column
                    title="类型"
                    data-index="sqlCommandType"
                    width="100"
                >
                    <template #default="{ text }">
                        <a-tag>{{ text }}</a-tag>
                    </template>
                </a-table-column>
                <a-table-column title="耗时" data-index="elapsedMs" width="110">
                    <template #default="{ record }">
                        <a-tag :color="record.slow ? 'red' : 'green'"
                            >{{ record.elapsedMs }} ms</a-tag
                        >
                    </template>
                </a-table-column>
                <a-table-column title="表名" data-index="tableName" width="160">
                    <template #default="{ text }">{{ text || '-' }}</template>
                </a-table-column>
                <a-table-column
                    title="结果"
                    data-index="resultSize"
                    :width="220"
                >
                    <template #default="{ text }">{{ text ?? '-' }}</template>
                </a-table-column>
                <a-table-column
                    title="traceId"
                    data-index="traceId"
                    width="220"
                >
                    <template #default="{ text }">
                        <span class="sql-trace-cell">{{ text || '-' }}</span>
                    </template>
                </a-table-column>
                <a-table-column title="SQL" data-index="sql" :width="360">
                    <template #default="{ record }">
                        <div class="sql-preview-cell">
                            <code class="sql-preview-text" :title="record.sql">
                                {{ record.sql }}
                            </code>
                            <a-button
                                type="text"
                                size="small"
                                title="查看 SQL"
                                aria-label="查看 SQL"
                                @click="openSqlDetail(record)"
                            >
                                查看
                            </a-button>
                            <a-button
                                type="text"
                                size="small"
                                title="复制 SQL"
                                aria-label="复制 SQL"
                                @click="copySql(record)"
                            >
                                <CopyOutlined />
                            </a-button>
                        </div>
                    </template>
                </a-table-column>
                <a-table-column
                    title="Mapper"
                    data-index="statementId"
                    width="260"
                >
                    <template #default="{ text }">
                        <span class="sql-statement-cell">{{ text }}</span>
                    </template>
                </a-table-column>
            </a-table>
        </div>
        <a-modal
            v-model:open="sqlDetailOpen"
            title="SQL 详情"
            :footer="null"
            width="820px"
        >
            <a-descriptions
                v-if="selectedSql"
                size="small"
                :column="2"
                bordered
            >
                <a-descriptions-item label="类型">{{
                    selectedSql.sqlCommandType
                }}</a-descriptions-item>
                <a-descriptions-item label="耗时"
                    >{{ selectedSql.elapsedMs }} ms</a-descriptions-item
                >
                <a-descriptions-item label="表名">{{
                    selectedSql.tableName || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="结果">{{
                    selectedSql.resultSize ?? '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="traceId" :span="2">{{
                    selectedSql.traceId || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="Mapper" :span="2">{{
                    selectedSql.statementId
                }}</a-descriptions-item>
            </a-descriptions>
            <pre v-if="selectedSql" class="sql-detail-content">{{
                selectedSql.sql
            }}</pre>
        </a-modal>
        <a-drawer
            v-model:open="mapperDrawerOpen"
            title="Mapper 采集范围"
            placement="right"
            :width="420"
        >
            <a-empty
                v-if="mapperTreeData.length === 0"
                description="暂无已发现 Mapper"
            />
            <a-spin v-else :spinning="settingsSaving">
                <a-tree
                    checkable
                    default-expand-all
                    :tree-data="mapperTreeData"
                    :checked-keys="checkedStatementIds"
                    @check="updateCheckedStatements"
                />
            </a-spin>
        </a-drawer>
    </section>
</template>

<style scoped>
.sql-query-bar {
    flex-wrap: wrap;
    align-items: center;
}

.sql-type-select {
    width: 140px;
}

.sql-limit-select {
    width: 140px;
}

.sql-keyword-search {
    min-width: 260px;
    flex: 1;
}

.sql-table-wrap {
    width: 100%;
    overflow-x: auto;
}

.sql-preview-cell {
    display: grid;
    grid-template-columns: minmax(180px, 1fr) auto auto;
    align-items: center;
    gap: 8px;
    width: 100%;
    min-width: 0;
}

.sql-preview-text,
.sql-trace-cell,
.sql-statement-cell {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.sql-preview-text {
    min-width: 0;
    color: var(--alpha-text);
}

.sql-detail-content {
    margin-top: 16px;
    max-height: 360px;
    overflow: auto;
    padding: 12px;
    border-radius: var(--alpha-radius);
    background: var(--alpha-canvas);
    white-space: pre-wrap;
    word-break: break-word;
}

@media (max-width: 767px) {
    .sql-query-bar {
        flex-direction: column;
        align-items: stretch;
    }

    .sql-type-select,
    .sql-limit-select,
    .sql-keyword-search {
        width: 100%;
        min-width: 0;
    }
}
</style>
