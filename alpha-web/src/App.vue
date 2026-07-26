<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const theme = {
    token: {
        colorPrimary: '#2563eb',
        colorInfo: '#2563eb',
        colorSuccess: '#16a34a',
        colorWarning: '#d97706',
        colorError: '#dc2626',
        colorText: '#172033',
        colorTextSecondary: '#667085',
        colorBorder: '#dfe3ea',
        borderRadius: 6,
        controlHeight: 36,
        fontFamily:
            "Inter, 'PingFang SC', 'Microsoft YaHei', ui-sans-serif, system-ui, sans-serif",
    },
}
</script>

<template>
    <a-config-provider :locale="zhCN" :theme="theme">
        <RouterView />
    </a-config-provider>
</template>
