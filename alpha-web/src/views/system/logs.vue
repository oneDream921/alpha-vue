<script setup lang="ts">
import {
    CheckOutlined,
    FileSearchOutlined,
    ReloadOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'

import TableActionMenu from '@/components/TableActionMenu.vue'
import { logApi, type LoginLog, type OperationLog } from '@/service/logs'
import { authStore } from '@/stores/auth'

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
function formatTime(value: string) {
    return value
        ? new Date(value).toLocaleString('zh-CN', { hour12: false })
        : '-'
}
function handlingStatusLabel(status: 0 | 1 | 2) {
    return ({ 0: '未处理', 1: '已处理', 2: '已忽略' } as const)[status]
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
function openDetail(record: OperationLog) {
    selectedOperation.value = record
    detailOpen.value = true
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
                                    record.status === 1 ? 'success' : 'error'
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
                            ><TableActionMenu
                                v-if="requiresHandling(record)"
                                aria-label="操作日志处理"
                                ><a-menu-item
                                    key="detail"
                                    @click="openDetail(record)"
                                    ><FileSearchOutlined />详情</a-menu-item
                                ><a-menu-item
                                    key="handled"
                                    v-permission="'log:operation:handle'"
                                    @click="updateHandlingStatus(record, 1)"
                                    ><CheckOutlined />已处理</a-menu-item
                                ><a-menu-item
                                    key="ignored"
                                    v-permission="'log:operation:handle'"
                                    @click="updateHandlingStatus(record, 2)"
                                    >已忽略</a-menu-item
                                ><a-menu-item
                                    v-if="record.handlingStatus !== 0"
                                    key="restore"
                                    v-permission="'log:operation:handle'"
                                    @click="updateHandlingStatus(record, 0)"
                                    >恢复</a-menu-item
                                ></TableActionMenu
                            ><span v-else class="log-no-action"
                                >无需操作</span
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
            </a-tab-pane>
            <a-tab-pane
                v-if="authStore.hasPermission('log:operation:list')"
                key="exceptions"
                tab="异常日志"
            >
                <div class="log-exception-hint">
                    只展示失败请求。默认未处理；定位并修复后可标为已处理，确认无需处理时可标为已忽略。
                </div>
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
                                    handlingStatusLabel(record.handlingStatus)
                                }}</a-tag
                            ></template
                        ></a-table-column
                    >
                    <a-table-column title="操作" width="220" align="center"
                        ><template #default="{ record }"
                            ><a-space :size="8"
                                ><a-button
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
            </a-tab-pane>
            <a-tab-pane
                v-if="authStore.hasPermission('log:login:list')"
                key="logins"
                tab="登录日志"
            >
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
            </a-tab-pane>
        </a-tabs>
        <a-modal
            v-model:open="detailOpen"
            title="异常详情"
            :footer="null"
            width="760px"
        >
            <a-descriptions
                v-if="selectedOperation"
                size="small"
                :column="2"
                bordered
            >
                <a-descriptions-item label="Trace ID">{{
                    selectedOperation.traceId || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="响应码">{{
                    selectedOperation.responseCode || '-'
                }}</a-descriptions-item>
                <a-descriptions-item label="请求"
                    >{{ selectedOperation.method }}
                    {{ selectedOperation.requestUri }}</a-descriptions-item
                >
                <a-descriptions-item label="状态">{{
                    handlingStatusLabel(selectedOperation.handlingStatus)
                }}</a-descriptions-item>
            </a-descriptions>
        </a-modal>
    </section>
</template>
