<script setup lang="ts">
import {
    ApartmentOutlined,
    DashboardOutlined,
    FileTextOutlined,
    FolderOpenOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuOutlined,
    MenuUnfoldOutlined,
    SafetyOutlined,
    SettingOutlined,
    UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '@/service/auth'
import { clearManagementRoutes } from '@/router'
import { authStore } from '@/stores/auth'
import logoUrl from '@/assets/alpha-logo.svg'

const router = useRouter()
const route = useRoute()

const viewportWidth = ref(
    typeof window === 'undefined' ? 1024 : window.innerWidth,
)
const mobileDrawerOpen = ref(false)
const desktopCollapsed = ref(viewportWidth.value < 1024)

const isMobile = computed(() => viewportWidth.value < 768)
const isDesktop = computed(() => viewportWidth.value >= 1024)
const sidebarCollapsed = computed(() =>
    isMobile.value ? true : desktopCollapsed.value,
)
const navigation = computed(() =>
    [
        { path: '/', title: '工作台', icon: DashboardOutlined },
        {
            path: '/system/users',
            title: '用户管理',
            icon: UserOutlined,
            permission: 'system:user:list',
        },
        {
            path: '/system/roles',
            title: '角色管理',
            icon: SafetyOutlined,
            permission: 'system:role:list',
        },
        {
            path: '/system/menus',
            title: '菜单管理',
            icon: MenuOutlined,
            permission: 'system:menu:list',
        },
        {
            path: '/system/depts',
            title: '部门管理',
            icon: ApartmentOutlined,
            permission: 'system:dept:list',
        },
        {
            path: '/system/configs',
            title: '参数配置',
            icon: SettingOutlined,
            permission: 'system:config:list',
        },
        {
            path: '/files',
            title: '文件管理',
            icon: FolderOpenOutlined,
            permission: 'file:list',
        },
        {
            path: '/logs',
            title: '审计日志',
            icon: FileTextOutlined,
            permission: 'log:operation:list',
        },
        { path: '/profile', title: '个人中心', icon: UserOutlined },
    ].filter(
        (item) => !item.permission || authStore.hasPermission(item.permission),
    ),
)
const currentNavigation = computed(() =>
    navigation.value.find((item) => item.path === route.path),
)
const displayName = computed(
    () =>
        authStore.state.profile?.nickname ||
        authStore.state.profile?.username ||
        '管理员',
)
const avatarText = computed(() => displayName.value.trim().slice(0, 1))

function updateViewport() {
    viewportWidth.value = window.innerWidth
    if (!isMobile.value) {
        mobileDrawerOpen.value = false
    }
    if (!isDesktop.value) {
        desktopCollapsed.value = true
    }
}

function closeMobileNavigation() {
    mobileDrawerOpen.value = false
}

async function logout() {
    try {
        await authApi.logout()
    } catch {
        message.warning('会话已在服务端失效')
    }
    authStore.clearAuth()
    clearManagementRoutes()
    await router.replace('/login')
}

onMounted(() => {
    updateViewport()
    window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => window.removeEventListener('resize', updateViewport))
</script>

<template>
    <a-layout class="base-layout">
        <a-layout-sider
            v-if="!isMobile"
            :collapsed="sidebarCollapsed"
            :collapsed-width="isDesktop ? 80 : 0"
            :trigger="null"
            class="desktop-sidebar"
        >
            <nav
                id="primary-navigation"
                aria-label="主导航"
                class="navigation-list"
            >
                <RouterLink
                    to="/"
                    class="brand"
                    :class="{ 'brand-collapsed': sidebarCollapsed }"
                    active-class=""
                    aria-label="Alpha Vue 首页"
                >
                    <img class="brand-mark" :src="logoUrl" alt="" />
                    <span v-if="!sidebarCollapsed" class="brand-name"
                        >Alpha Vue</span
                    >
                </RouterLink>
                <RouterLink
                    v-for="item in navigation"
                    :key="item.path"
                    :to="item.path"
                    class="navigation-link"
                    :class="{
                        'navigation-link-active': route.path === item.path,
                    }"
                    active-class=""
                    :title="sidebarCollapsed ? item.title : undefined"
                    ><component :is="item.icon" /><span
                        v-if="!sidebarCollapsed"
                        >{{ item.title }}</span
                    ></RouterLink
                >
            </nav>
        </a-layout-sider>

        <a-drawer
            v-model:open="mobileDrawerOpen"
            placement="left"
            :width="280"
            title="导航"
            :body-style="{ padding: '12px' }"
        >
            <nav
                data-testid="mobile-navigation"
                aria-label="移动主导航"
                class="mobile-navigation-list"
            >
                <RouterLink
                    v-for="item in navigation"
                    :key="item.path"
                    :to="item.path"
                    class="navigation-link"
                    :class="{
                        'navigation-link-active': route.path === item.path,
                    }"
                    active-class=""
                    @click="closeMobileNavigation"
                    ><component :is="item.icon" /><span>{{
                        item.title
                    }}</span></RouterLink
                >
            </nav>
        </a-drawer>

        <a-layout>
            <a-layout-header class="app-header">
                <a-button
                    v-if="isMobile"
                    data-testid="mobile-nav-trigger"
                    type="text"
                    aria-label="打开导航"
                    @click="mobileDrawerOpen = true"
                >
                    <MenuUnfoldOutlined />
                </a-button>
                <a-button
                    v-else
                    type="text"
                    :aria-controls="'primary-navigation'"
                    :aria-expanded="!sidebarCollapsed"
                    :aria-label="sidebarCollapsed ? '展开侧栏' : '折叠侧栏'"
                    @click="desktopCollapsed = !desktopCollapsed"
                >
                    <MenuUnfoldOutlined v-if="sidebarCollapsed" />
                    <MenuFoldOutlined v-else />
                </a-button>
                <div class="header-context">
                    <span class="header-product">Alpha Vue</span>
                    <span class="header-separator">/</span>
                    <strong>{{
                        currentNavigation?.title || '管理控制台'
                    }}</strong>
                </div>
                <span class="header-spacer" />
                <div class="header-user-cluster">
                    <a-avatar
                        :size="30"
                        :src="authStore.state.profile?.avatar"
                        class="header-avatar"
                        >{{ avatarText }}</a-avatar
                    >
                    <span class="header-user">{{ displayName }}</span>
                </div>
                <a-button
                    type="text"
                    aria-label="退出登录"
                    title="退出登录"
                    @click="logout"
                    ><LogoutOutlined
                /></a-button>
            </a-layout-header>
            <a-layout-content class="app-content">
                <RouterView />
            </a-layout-content>
        </a-layout>
    </a-layout>
</template>
