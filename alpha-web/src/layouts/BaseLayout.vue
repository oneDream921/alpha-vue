<script setup lang="ts">
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

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
                <RouterLink to="/" class="brand" aria-label="Alpha Vue 首页"
                    >AV</RouterLink
                >
                <RouterLink to="/" class="navigation-link">工作台</RouterLink>
                <RouterLink to="/profile" class="navigation-link"
                    >个人中心</RouterLink
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
                    to="/"
                    class="navigation-link"
                    @click="closeMobileNavigation"
                    >工作台</RouterLink
                >
                <RouterLink
                    to="/profile"
                    class="navigation-link"
                    @click="closeMobileNavigation"
                    >个人中心</RouterLink
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
                <span class="app-title">Alpha Vue</span>
            </a-layout-header>
            <a-layout-content class="app-content">
                <RouterView />
            </a-layout-content>
        </a-layout>
    </a-layout>
</template>
