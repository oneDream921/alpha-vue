<script setup lang="ts">
import {
    CheckOutlined,
    CopyOutlined,
    FileSearchOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

import AlphaTableCard from '@/components/AlphaTableCard.vue'
import TableActionMenu from '@/components/TableActionMenu.vue'
import {
    logApi,
    type LoginLog,
    type OperationLog,
    type OperationLogDetail,
} from '@/service/logs'
import { authStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/dateTime'

const operationRows = ref<OperationLog[]>([])
const loginRows = ref<LoginLog[]>([])
const exceptionRows = ref<OperationLog[]>([])
const operationPage = ref(1),
    operationTotal = ref(0)
const loginPage = ref(1),
    loginTotal = ref(0)
const exceptionPage = ref(1),
    exceptionTotal = ref(0)
const size = 10
const loading = ref(false)
const detailOpen = ref(false)
const selectedOperation = ref<OperationLog>()
const selectedDetail = ref<OperationLogDetail>()
function formatAuditSummary(summary?: string) {
    if (!summary) return '未采集'
    try {
        return JSON.stringify(JSON.parse(summary), null, 2)
    } catch {
        return summary
    }
}
type AuditSummaryToken = {
    text: string
    kind: 'key' | 'string' | 'number' | 'literal' | 'punctuation' | 'plain'
}
function tokenizeAuditSummary(summary?: string): AuditSummaryToken[] {
    const formatted = formatAuditSummary(summary)
    if (formatted === '未采集') return [{ text: formatted, kind: 'plain' }]
    const tokens: AuditSummaryToken[] = []
    const pattern =
        /("(?:\\.|[^"\\])*")(?=\s*:)|("(?:\\.|[^"\\])*")|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(true|false|null)|([{}[\],:])|(\s+)|(.+)/g
    let match: RegExpExecArray | null
    while ((match = pattern.exec(formatted)) !== null) {
        const [text, key, string, number, literal, punctuation] = match
        tokens.push({
            text,
            kind: key
                ? 'key'
                : string
                  ? 'string'
                  : number
                    ? 'number'
                    : literal
                      ? 'literal'
                      : punctuation
                        ? 'punctuation'
                        : 'plain',
        })
    }
    return tokens
}
const requestSummaryTokens = computed(() =>
    tokenizeAuditSummary(selectedDetail.value?.requestSummary),
)
const responseSummaryTokens = computed(() =>
    tokenizeAuditSummary(selectedDetail.value?.responseSummary),
)
const query = reactive({
    keyword: '',
    result: undefined as 0 | 1 | undefined,
    handlingStatus: undefined as 0 | 1 | 2 | undefined,
})
function search() {
    operationPage.value = 1
    exceptionPage.value = 1
    void Promise.all([loadOperations(), loadExceptions()])
}
function resetQuery() {
    Object.assign(query, {
        keyword: '',
        result: undefined,
        handlingStatus: undefined,
    })
    search()
}
async function loadOperations() {
    loading.value = true
    try {
        const response = await logApi.operations(operationPage.value, size, {
            keyword: query.keyword || undefined,
            status: query.result,
            handlingStatus: query.handlingStatus,
        })
        operationRows.value = response.data.data.records
        operationTotal.value = response.data.data.total
    } finally {
        loading.value = false
    }
}
async function loadExceptions() {
    loading.value = true
    try {
        const response = await logApi.operations(exceptionPage.value, size, {
            keyword: query.keyword || undefined,
            status: 0,
            handlingStatus: query.handlingStatus,
        })
        exceptionRows.value = response.data.data.records
        exceptionTotal.value = response.data.data.total
    } finally {
        loading.value = false
    }
}
async function loadLogins() {
    loading.value = true
    try {
        const response = await logApi.logins(loginPage.value, size)
        loginRows.value = response.data.data.records
        loginTotal.value = response.data.data.total
    } finally {
        loading.value = false
    }
}
function changeOperationPage(pagination: { current?: number }) {
    operationPage.value = pagination.current ?? 1
    void loadOperations()
}
function changeLoginPage(pagination: { current?: number }) {
    loginPage.value = pagination.current ?? 1
    void loadLogins()
}
function changeExceptionPage(pagination: { current?: number }) {
    exceptionPage.value = pagination.current ?? 1
    void loadExceptions()
}
const formatTime = formatDateTime
function handlingStatusLabel(status: 0 | 1 | 2) {
    return ({ 0: '未处理', 1: '已处理', 2: '已忽略' } as const)[status]
}
function handlingStatusColor(status: 0 | 1 | 2) {
    return ({ 0: 'warning', 1: 'success', 2: 'default' } as const)[status]
}
function requestMethodColor(method?: string) {
    return (
        (
            {
                DELETE: 'error',
                GET: 'success',
                PATCH: 'purple',
                POST: 'blue',
                PUT: 'orange',
            } as const
        )[
            method?.toUpperCase() as 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'
        ] ?? 'blue'
    )
}
function requiresHandling(record: OperationLog) {
    return record.status === 0
}
async function refreshLogs() {
    const jobs: Promise<void>[] = []
    if (authStore.hasPermission('log:operation:list'))
        jobs.push(loadOperations(), loadExceptions())
    if (authStore.hasPermission('log:login:list')) jobs.push(loadLogins())
    await Promise.all(jobs)
}
async function openDetail(record: OperationLog) {
    if (!authStore.hasPermission('log:operation:detail')) {
        message.error('没有查看详情权限')
        return
    }
    const response = await logApi.operationDetail(record.id)
    selectedDetail.value = response.data.data
    selectedOperation.value = response.data.data.summary
    detailOpen.value = true
}
async function copyExceptionStack() {
    const exceptionStack = selectedDetail.value?.exceptionStack
    if (!exceptionStack) return
    try {
        await window.navigator.clipboard.writeText(exceptionStack)
        message.success('异常堆栈已复制')
    } catch {
        message.error('复制失败，请手动复制')
    }
}
async function updateHandlingStatus(
    record: OperationLog,
    handlingStatus: 0 | 1 | 2,
) {
    await logApi.updateHandlingStatus(record.id, handlingStatus)
    message.success(
        handlingStatus === 1
            ? '已标记为已处理'
            : handlingStatus === 2
              ? '已标记为已忽略'
              : '已恢复为未处理',
    )
    await Promise.all([loadOperations(), loadExceptions()])
    if (selectedOperation.value?.id === record.id)
        selectedOperation.value.handlingStatus = handlingStatus
}
onMounted(refreshLogs)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>审计日志</h1>
                <p>查询登录事件与管理操作元数据</p>
            </div>
            <a-button @click="refreshLogs"><ReloadOutlined />刷新</a-button>
        </div>
        <a-form class="log-query-bar" layout="inline">
            <a-form-item
                ><a-input
                    v-model:value="query.keyword"
                    allow-clear
                    placeholder="账号、模块、操作或接口"
            /></a-form-item>
            <a-form-item
                ><a-select
                    v-model:value="query.result"
                    allow-clear
                    placeholder="请求结果"
                    style="width: 120px"
                    ><a-select-option :value="1">成功</a-select-option
                    ><a-select-option :value="0"
                        >失败</a-select-option
                    ></a-select
                ></a-form-item
            >
            <a-form-item
                ><a-select
                    v-model:value="query.handlingStatus"
                    allow-clear
                    placeholder="异常状态"
                    style="width: 130px"
                    ><a-select-option :value="0">未处理</a-select-option
                    ><a-select-option :value="1">已处理</a-select-option
                    ><a-select-option :value="2"
                        >已忽略</a-select-option
                    ></a-select
                ></a-form-item
            >
            <a-form-item
                ><a-space :size="8"
                    ><a-button type="primary" @click="search">搜索</a-button
                    ><a-button @click="resetQuery">重置</a-button></a-space
                ></a-form-item
            >
        </a-form>
        <a-tabs>
            <a-tab-pane
                v-if="authStore.hasPermission('log:operation:list')"
                key="operations"
                tab="操作日志"
            >
                <AlphaTableCard :loading="loading">
                    <a-table
                        row-key="id"
                        :data-source="operationRows"
                        :loading="loading"
                        :pagination="{
                            current: operationPage,
                            pageSize: size,
                            total: operationTotal,
                        }"
                        :scroll="{ x: 1280 }"
                        @change="changeOperationPage"
                    >
                        <a-table-column
                            title="用户"
                            data-index="username"
                            width="120"
                            ><template #default="{ text }">{{
                                text || '-'
                            }}</template></a-table-column
                        ><a-table-column
                            title="模块"
                            data-index="module"
                            width="120"
                        /><a-table-column
                            title="操作"
                            data-index="operation"
                            width="220"
                        /><a-table-column
                            title="请求"
                            data-index="requestUri"
                            width="220"
                            ><template #default="{ record }"
                                >{{ record.method }}
                                {{ record.requestUri }}</template
                            ></a-table-column
                        ><a-table-column
                            title="状态"
                            data-index="status"
                            width="90"
                            align="center"
                            ><template #default="{ record }"
                                ><a-badge
                                    :status="
                                        record.status === 1
                                            ? 'success'
                                            : 'error'
                                    "
                                    :text="
                                        String(record.responseCode ?? '-')
                                    " /></template></a-table-column
                        ><a-table-column
                            title="处理状态"
                            data-index="handled"
                            width="110"
                            align="center"
                            ><template #default="{ record }"
                                ><a-tag
                                    :color="
                                        requiresHandling(record)
                                            ? record.handlingStatus === 1
                                                ? 'success'
                                                : record.handlingStatus === 2
                                                  ? 'default'
                                                  : 'warning'
                                            : 'blue'
                                    "
                                    >{{
                                        requiresHandling(record)
                                            ? handlingStatusLabel(
                                                  record.handlingStatus,
                                              )
                                            : '无需处理'
                                    }}</a-tag
                                ></template
                            ></a-table-column
                        ><a-table-column title="操作" width="88" align="center"
                            ><template #default="{ record }"
                                ><TableActionMenu aria-label="操作日志操作"
                                    ><a-menu-item
                                        key="detail"
                                        v-permission="'log:operation:detail'"
                                        @click="openDetail(record)"
                                        ><FileSearchOutlined />详情</a-menu-item
                                    ><a-menu-item
                                        v-if="requiresHandling(record)"
                                        key="handled"
                                        v-permission="'log:operation:handle'"
                                        @click="updateHandlingStatus(record, 1)"
                                        ><CheckOutlined />已处理</a-menu-item
                                    ><a-menu-item
                                        v-if="requiresHandling(record)"
                                        key="ignored"
                                        v-permission="'log:operation:handle'"
                                        @click="updateHandlingStatus(record, 2)"
                                        >已忽略</a-menu-item
                                    ><a-menu-item
                                        v-if="
                                            requiresHandling(record) &&
                                            record.handlingStatus !== 0
                                        "
                                        key="restore"
                                        v-permission="'log:operation:handle'"
                                        @click="updateHandlingStatus(record, 0)"
                                        >恢复</a-menu-item
                                    ></TableActionMenu
                                ></template
                            ></a-table-column
                        ><a-table-column
                            title="耗时"
                            data-index="durationMs"
                            width="120"
                            align="center"
                            ><template #default="{ text }"
                                >{{ text ?? '-' }} ms</template
                            ></a-table-column
                        ><a-table-column
                            title="IP"
                            data-index="ipAddress"
                            width="150"
                        /><a-table-column
                            title="Trace ID"
                            data-index="traceId"
                            width="290"
                            ><template #default="{ text }"
                                ><a-typography-text copyable>{{
                                    text || '-'
                                }}</a-typography-text></template
                            ></a-table-column
                        ><a-table-column
                            title="时间"
                            data-index="createdAt"
                            width="190"
                            ><template #default="{ text }">{{
                                formatTime(text)
                            }}</template></a-table-column
                        >
                    </a-table>
                </AlphaTableCard>
            </a-tab-pane>
            <a-tab-pane
                v-if="authStore.hasPermission('log:operation:list')"
                key="exceptions"
                tab="异常日志"
            >
                <div class="log-exception-hint">
                    只展示失败请求。默认未处理；定位并修复后可标为已处理，确认无需处理时可标为已忽略。
                </div>
                <AlphaTableCard :loading="loading">
                    <a-table
                        row-key="id"
                        :data-source="exceptionRows"
                        :loading="loading"
                        :pagination="{
                            current: exceptionPage,
                            pageSize: size,
                            total: exceptionTotal,
                        }"
                        :scroll="{ x: 920 }"
                        @change="changeExceptionPage"
                    >
                        <a-table-column title="模块 / 操作" width="210"
                            ><template #default="{ record }"
                                >{{ record.module }} /
                                {{ record.operation }}</template
                            ></a-table-column
                        >
                        <a-table-column title="请求" width="230"
                            ><template #default="{ record }"
                                >{{ record.method }}
                                {{ record.requestUri }}</template
                            ></a-table-column
                        >
                        <a-table-column
                            title="响应码"
                            data-index="responseCode"
                            width="90"
                            align="center"
                        />
                        <a-table-column title="状态" width="100" align="center"
                            ><template #default="{ record }"
                                ><a-tag
                                    :color="
                                        record.handlingStatus === 1
                                            ? 'success'
                                            : record.handlingStatus === 2
                                              ? 'default'
                                              : 'warning'
                                    "
                                    >{{
                                        handlingStatusLabel(
                                            record.handlingStatus,
                                        )
                                    }}</a-tag
                                ></template
                            ></a-table-column
                        >
                        <a-table-column title="操作" width="220" align="center"
                            ><template #default="{ record }"
                                ><a-space :size="8"
                                    ><a-button
                                        v-permission="'log:operation:detail'"
                                        type="link"
                                        size="small"
                                        @click="openDetail(record)"
                                        ><FileSearchOutlined />详情</a-button
                                    ><a-button
                                        v-permission="'log:operation:handle'"
                                        type="link"
                                        size="small"
                                        @click="updateHandlingStatus(record, 1)"
                                        >已处理</a-button
                                    ><a-button
                                        v-permission="'log:operation:handle'"
                                        type="link"
                                        size="small"
                                        @click="updateHandlingStatus(record, 2)"
                                        >已忽略</a-button
                                    ></a-space
                                ></template
                            ></a-table-column
                        >
                        <a-table-column
                            title="时间"
                            data-index="createdAt"
                            width="190"
                            ><template #default="{ text }">{{
                                formatTime(text)
                            }}</template></a-table-column
                        >
                    </a-table>
                </AlphaTableCard>
            </a-tab-pane>
            <a-tab-pane
                v-if="authStore.hasPermission('log:login:list')"
                key="logins"
                tab="登录日志"
            >
                <AlphaTableCard :loading="loading">
                    <a-table
                        row-key="id"
                        :data-source="loginRows"
                        :loading="loading"
                        :pagination="{
                            current: loginPage,
                            pageSize: size,
                            total: loginTotal,
                        }"
                        :scroll="{ x: 850 }"
                        @change="changeLoginPage"
                    >
                        <a-table-column
                            title="账号"
                            data-index="username"
                            width="160"
                        /><a-table-column
                            title="类型"
                            data-index="loginType"
                            width="120"
                        /><a-table-column
                            title="结果"
                            data-index="status"
                            width="100"
                            align="center"
                            ><template #default="{ text }"
                                ><a-badge
                                    :status="text === 1 ? 'success' : 'error'"
                                    :text="
                                        text === 1 ? '成功' : '失败'
                                    " /></template></a-table-column
                        ><a-table-column
                            title="IP"
                            data-index="ipAddress"
                            width="160"
                        /><a-table-column
                            title="消息"
                            data-index="message"
                            width="220"
                        /><a-table-column
                            title="时间"
                            data-index="createdAt"
                            width="190"
                            ><template #default="{ text }">{{
                                formatTime(text)
                            }}</template></a-table-column
                        >
                    </a-table>
                </AlphaTableCard>
            </a-tab-pane>
        </a-tabs>
        <a-modal
            v-model:open="detailOpen"
            title="操作日志详情"
            width="min(860px, calc(100vw - 32px))"
        >
            <a-descriptions
                v-if="selectedOperation"
                class="log-detail-descriptions"
                size="small"
                :column="1"
                bordered
            >
                <a-descriptions-item label="Trace ID">{{
                    selectedOperation.traceId || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="响应码">{{
                    selectedOperation.responseCode || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="请求">
                    <a-space class="log-request-info" size="small">
                        <a-tag
                            :color="
                                requestMethodColor(selectedOperation.method)
                            "
                        >
                            {{ selectedOperation.method || '未知' }} 请求
                        </a-tag>
                        <span>{{ selectedOperation.requestUri || '-' }}</span>
                    </a-space>
                </a-descriptions-item>
                <a-descriptions-item label="客户端">{{
                    selectedOperation.clientId || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="地点 / IP"
                    >{{ selectedOperation.location || '-' }} /
                    {{
                        selectedOperation.ipAddress || '-'
                    }}</a-descriptions-item
                >
                <a-descriptions-item label="浏览器 / 系统"
                    >{{ selectedOperation.browser || '-' }} /
                    {{
                        selectedOperation.operatingSystem || '-'
                    }}</a-descriptions-item
                >
                <a-descriptions-item label="业务错误码">{{
                    selectedOperation.errorCode ?? '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="状态">
                    <a-tag
                        :color="
                            requiresHandling(selectedOperation)
                                ? handlingStatusColor(
                                      selectedOperation.handlingStatus,
                                  )
                                : 'blue'
                        "
                    >
                        {{
                            requiresHandling(selectedOperation)
                                ? handlingStatusLabel(
                                      selectedOperation.handlingStatus,
                                  )
                                : '无需处理'
                        }}
                    </a-tag>
                </a-descriptions-item>
                <a-descriptions-item :span="2">
                    <template #label>
                        <span class="exception-stack-label">
                            <span>异常堆栈</span>
                            <a-button
                                type="text"
                                size="small"
                                class="exception-copy-button"
                                aria-label="复制异常堆栈"
                                title="复制异常堆栈"
                                :disabled="!selectedDetail?.exceptionStack"
                                @click="copyExceptionStack"
                            >
                                <CopyOutlined />
                            </a-button>
                        </span>
                    </template>
                    <pre class="exception-stack">{{
                        selectedDetail?.exceptionStack || '未记录异常堆栈'
                    }}</pre>
                </a-descriptions-item>
                <a-descriptions-item label="请求摘要" :span="2">
                    <pre class="audit-summary"><code><span
                        v-for="(token, index) in requestSummaryTokens"
                        :key="index"
                        class="audit-summary-token"
                        :class="'audit-summary-token-' + token.kind"
                    >{{ token.text }}</span></code></pre>
                </a-descriptions-item>
                <a-descriptions-item label="响应摘要" :span="2">
                    <pre class="audit-summary"><code><span
                        v-for="(token, index) in responseSummaryTokens"
                        :key="index"
                        class="audit-summary-token"
                        :class="'audit-summary-token-' + token.kind"
                    >{{ token.text }}</span></code></pre>
                </a-descriptions-item>
            </a-descriptions>
            <template #footer>
                <div class="log-detail-footer">
                    <a-button @click="detailOpen = false">关闭</a-button>
                </div>
            </template>
        </a-modal>
    </section>
</template>

<style scoped>
.log-detail-descriptions :deep(.ant-descriptions-item-content) {
    min-width: 0;
    overflow: hidden;
}

.log-detail-descriptions :deep(.ant-descriptions-item-label) {
    width: 150px;
    min-width: 150px;
    vertical-align: top;
}

.log-detail-descriptions.log-detail-descriptions
    :deep(.ant-descriptions-view > table) {
    width: 100%;
    max-width: 100% !important;
    table-layout: fixed !important;
}

.log-detail-descriptions :deep(.ant-descriptions-view) {
    overflow: hidden;
}

.log-detail-footer {
    display: flex;
    justify-content: flex-end;
}

.log-request-info {
    max-width: 100%;
    flex-wrap: wrap;
}

.log-request-info > span {
    min-width: 0;
    overflow-wrap: anywhere;
}

.exception-stack {
    box-sizing: border-box;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    max-height: 320px;
    margin: 0;
    padding: 0;
    overflow: auto;
    border: 0;
    background: transparent;
    color: var(--alpha-text);
    font-family: inherit;
    font-size: inherit;
    line-height: inherit;
    white-space: pre;
}

.exception-stack-label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.exception-copy-button {
    width: 24px;
    height: 24px;
    padding: 0;
}

.audit-summary {
    box-sizing: border-box;
    width: 100%;
    max-height: 180px;
    margin: 0;
    padding: 0;
    overflow: auto;
    border: 0;
    background: transparent;
    color: var(--alpha-text);
    font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
    font-size: 14px;
    line-height: 1.7;
    white-space: pre;
}

.audit-summary-token-key {
    color: #a66a00;
    font-weight: 500;
}

.audit-summary-token-string {
    color: #4f9d50;
}

.audit-summary-token-number {
    color: #2563a8;
}

.audit-summary-token-literal {
    color: #b42318;
    font-weight: 600;
}

.audit-summary-token-punctuation {
    color: #30343b;
}

@media (max-width: 767px) {
    .log-detail-descriptions :deep(.ant-descriptions-item-label) {
        width: 96px;
        min-width: 96px;
    }
}
</style>
