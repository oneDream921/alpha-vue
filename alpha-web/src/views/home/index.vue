<script setup lang="ts">
import {
    DatabaseOutlined,
    FileOutlined,
    SettingOutlined,
    TeamOutlined,
} from '@ant-design/icons-vue'
import { computed } from 'vue'

import logoUrl from '@/assets/alpha-logo.svg'
import { authStore } from '@/stores/auth'

const displayName = computed(
    () =>
        authStore.state.profile?.nickname ||
        authStore.state.profile?.username ||
        '管理员',
)

const quickLinks = computed(() =>
    [
        {
            title: '用户管理',
            description: '维护账号、角色与登录状态',
            path: '/system/users',
            permission: 'system:user:list',
            icon: TeamOutlined,
        },
        {
            title: '系统配置',
            description: '管理站点、存储与安全参数',
            path: '/system/settings',
            permission: 'system:setting:list',
            icon: SettingOutlined,
        },
        {
            title: 'Redis 管理',
            description: '查看缓存概览与运行指标',
            path: '/monitor/redis',
            permission: 'monitor:redis:list',
            icon: DatabaseOutlined,
        },
        {
            title: '文件管理',
            description: '浏览和维护平台文件资源',
            path: '/files',
            permission: 'file:list',
            icon: FileOutlined,
        },
    ].filter((item) => authStore.hasPermission(item.permission)),
)
</script>

<template>
    <section class="page-section dashboard-page">
        <section class="dashboard-hero" aria-labelledby="dashboard-title">
            <div class="dashboard-hero-copy">
                <span class="dashboard-eyebrow">ALPHA VUE 管理平台</span>
                <h1 id="dashboard-title">欢迎回来，{{ displayName }}</h1>
                <p>从常用入口开始，快速管理你的业务与系统资源。</p>
            </div>
            <div class="dashboard-hero-art" aria-hidden="true">
                <img :src="logoUrl" alt="" />
            </div>
            <div class="dashboard-summary">
                <div>
                    <span>当前会话</span>
                    <strong>安全连接</strong>
                </div>
                <div>
                    <span>可访问模块</span>
                    <strong>{{ quickLinks.length }} 个</strong>
                </div>
            </div>
        </section>
        <div class="dashboard-section-heading">
            <div>
                <h2>常用入口</h2>
                <p>根据当前权限为你展示可用功能。</p>
            </div>
        </div>
        <div v-if="quickLinks.length" class="dashboard-grid">
            <RouterLink
                v-for="link in quickLinks"
                :key="link.path"
                :to="link.path"
                class="quick-link"
            >
                <span class="quick-link-icon"
                    ><component :is="link.icon"
                /></span>
                <span class="quick-link-content">
                    <strong>{{ link.title }}</strong>
                    <span>{{ link.description }}</span>
                </span>
                <span class="quick-link-arrow" aria-hidden="true">→</span>
            </RouterLink>
        </div>
        <a-empty v-else description="暂无可访问的功能" />
    </section>
</template>
