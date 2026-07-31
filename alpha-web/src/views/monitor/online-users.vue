<script setup lang="ts">
import { DisconnectOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { onMounted, ref } from 'vue'

import { onlineApi, type OnlineSession } from '@/service/online'
import { authStore } from '@/stores/auth'

const rows = ref<OnlineSession[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

function formatTime(value: string) {
    return value
        ? new Date(value).toLocaleString('zh-CN', { hour12: false })
        : '-'
}

async function load() {
    loading.value = true
    try {
        const response = await onlineApi.page(page.value, size.value)
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

function kickout(row: OnlineSession) {
    const username = row.user?.username ?? '该会话'
    Modal.confirm({
        title: `强制“${username}”退出当前会话？`,
        content: '只会结束选中的客户端会话，其他客户端不会受到影响。',
        okType: 'danger',
        async onOk() {
            await onlineApi.kickout(row.userId, row.terminalIndex)
            message.success('会话已下线')
            await load()
        },
    })
}

onMounted(load)
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>在线用户</h1>
                <p>按客户端会话查看在线账号并执行定向下线</p>
            </div>
            <a-button :loading="loading" @click="load"
                ><ReloadOutlined />刷新</a-button
            >
        </div>
        <a-table
            row-key="terminalIndex"
            :data-source="rows"
            :loading="loading"
            :scroll="{ x: 1280 }"
            :pagination="{
                current: page,
                pageSize: size,
                total,
                showSizeChanger: true,
            }"
            @change="changePage"
        >
            <a-table-column title="账号" width="150">
                <template #default="{ record }">
                    {{ record.user?.username ?? '-' }}
                    <span class="table-secondary">{{
                        record.user?.nickname
                    }}</span>
                </template>
            </a-table-column>
            <a-table-column title="客户端" data-index="clientId" width="130" />
            <a-table-column title="设备" width="180">
                <template #default="{ record }">
                    {{
                        record.deviceName ||
                        record.deviceId ||
                        [record.browser, record.operatingSystem]
                            .filter((value) => value && value !== '未知')
                            .join(' / ') ||
                        '-'
                    }}
                </template>
            </a-table-column>
            <a-table-column title="网络" width="170">
                <template #default="{ record }">
                    {{ record.ipAddress || '-' }}
                    <span class="table-secondary"
                        >{{ record.browser }} /
                        {{ record.operatingSystem }}</span
                    >
                </template>
            </a-table-column>
            <a-table-column title="登录时间" width="180">
                <template #default="{ record }">{{
                    formatTime(record.loginTime)
                }}</template>
            </a-table-column>
            <a-table-column title="最后访问" width="180">
                <template #default="{ record }">{{
                    formatTime(record.lastActiveTime)
                }}</template>
            </a-table-column>
            <a-table-column
                title="会话标识"
                data-index="tokenSummary"
                width="150"
            />
            <a-table-column
                title="操作"
                fixed="right"
                width="110"
                align="center"
            >
                <template #default="{ record }">
                    <a-button
                        v-if="authStore.hasPermission('monitor:online:kickout')"
                        type="link"
                        danger
                        @click="kickout(record)"
                        ><DisconnectOutlined />下线</a-button
                    >
                </template>
            </a-table-column>
        </a-table>
    </section>
</template>

<style scoped>
.table-secondary {
    display: block;
    color: var(--alpha-text-secondary);
    font-size: 12px;
}
</style>
