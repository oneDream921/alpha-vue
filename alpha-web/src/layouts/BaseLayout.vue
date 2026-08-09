<script setup lang="ts">
import {
    CloseOutlined,
    DownOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    RightOutlined,
    SearchOutlined,
    UserOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '@/service/auth/index'
import { systemSettingApi } from '@/service/system/settings'
import { clearManagementRoutes } from '@/router'
import { authStore } from '@/stores/auth'
import logoUrl from '@/assets/alpha-logo.svg'
import AppBreadcrumb from './AppBreadcrumb.vue'
import {
    buildNavigation,
    closeTabAt,
    closeTabsExcept,
    closeTabsLeftOf,
    closeTabsRightOf,
    flattenNavigationLeaves,
    tabTitleForPath,
    type OpenTab,
} from './navigation'

const router = useRouter()
const route = useRoute()

interface MenuClickEvent {
    key: string | number
}

interface MenuSearchOption {
    label: string
    value: string
}

type ScrollableContent = { scrollLeft: number; scrollTop: number }
type FocusableInput = ComponentPublicInstance & { focus?: () => void }
type LayoutContentRef =
    ScrollableContent | (ComponentPublicInstance & { $el?: ScrollableContent })

const viewportWidth = ref(
    typeof window === 'undefined' ? 1024 : window.innerWidth,
)
const mobileDrawerOpen = ref(false)
const desktopCollapsed = ref(viewportWidth.value < 1024)
const openTabs = ref<OpenTab[]>([])
const menuSearchValue = ref('')
const menuSearchOpen = ref(false)
const expandedNavigationGroups = ref<string[]>([])
const appContentRef = ref<LayoutContentRef | null>(null)
const menuSearchRef = ref<FocusableInput | null>(null)
const siteInfo = ref({ copyright: '', icp: '' })
const watermark = ref({
    enabled: false,
    type: 'custom' as 'custom' | 'username',
    content: '',
    opacity: 0.12,
})

const isMobile = computed(() => viewportWidth.value < 768)
const isDesktop = computed(() => viewportWidth.value >= 1024)
const sidebarCollapsed = computed(() =>
    isMobile.value ? true : desktopCollapsed.value,
)
const navigation = computed(() =>
    buildNavigation(authStore.state.routes, authStore.hasPermission),
)
const navigationLeaves = computed(() =>
    flattenNavigationLeaves(navigation.value),
)
const visibleNavigation = computed(() =>
    sidebarCollapsed.value ? navigationLeaves.value : navigation.value,
)
const menuSearchOptions = computed<MenuSearchOption[]>(() =>
    navigationLeaves.value.map((item) => ({
        label: item.title,
        value: item.path,
    })),
)
const filteredMenuSearchOptions = computed(() => {
    const keyword = menuSearchValue.value.trim().toLowerCase()
    return keyword
        ? menuSearchOptions.value.filter((item) =>
              item.label.toLowerCase().includes(keyword),
          )
        : menuSearchOptions.value
})
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

function isActivePath(path: string) {
    return route.path === path
}

function isActiveTab(path: string) {
    return route.fullPath === path
}

function isNavigationGroupExpanded(key: string) {
    return expandedNavigationGroups.value.includes(key)
}

function toggleNavigationGroup(key: string) {
    expandedNavigationGroups.value = isNavigationGroupExpanded(key)
        ? expandedNavigationGroups.value.filter((item) => item !== key)
        : [...expandedNavigationGroups.value, key]
}

function hasActiveChild(children: readonly { path?: string }[] | undefined) {
    return children?.some((child) => child.path && isActivePath(child.path))
}

function expandActiveNavigationGroup() {
    const activeGroup = navigation.value.find((item) =>
        item.children?.some((child) => child.path === route.path),
    )
    if (
        activeGroup?.children &&
        !expandedNavigationGroups.value.includes(activeGroup.key)
    ) {
        expandedNavigationGroups.value = [
            ...expandedNavigationGroups.value,
            activeGroup.key,
        ]
    }
}

function addCurrentTab() {
    if (route.meta.requiresAuth === false || route.name === 'login') {
        return
    }

    const title =
        typeof route.meta.title === 'string' ? route.meta.title : '未命名页面'
    const tab: OpenTab = {
        path: route.fullPath,
        title: tabTitleForPath(route.path, navigation.value, title),
    }
    const existingIndex = openTabs.value.findIndex(
        (item) => item.path === tab.path,
    )

    if (existingIndex >= 0) {
        openTabs.value.splice(existingIndex, 1, tab)
        return
    }

    openTabs.value.push(tab)
}

function replacementAfterClose(
    beforeClose: readonly OpenTab[],
    remaining: readonly OpenTab[],
    targetPath: string,
) {
    const targetIndex = beforeClose.findIndex((tab) => tab.path === targetPath)
    return remaining[Math.min(targetIndex, remaining.length - 1)]?.path ?? '/'
}

async function closeTab(targetPath: string) {
    const beforeClose = [...openTabs.value]
    const remaining = closeTabAt(beforeClose, targetPath)
    openTabs.value = remaining

    if (isActiveTab(targetPath)) {
        await router.replace(
            replacementAfterClose(beforeClose, remaining, targetPath),
        )
    }
}

async function closeLeftTabs(targetPath: string) {
    const activePath = route.fullPath
    const beforeClose = [...openTabs.value]
    const remaining = closeTabsLeftOf(beforeClose, targetPath)
    openTabs.value = remaining

    if (!remaining.some((tab) => tab.path === activePath)) {
        await router.replace(targetPath)
    }
}

async function closeOtherTabs(targetPath: string) {
    openTabs.value = closeTabsExcept(openTabs.value, targetPath)

    if (!isActiveTab(targetPath)) {
        await router.replace(targetPath)
    }
}

async function closeRightTabs(targetPath: string) {
    const remaining = closeTabsRightOf(openTabs.value, targetPath)
    openTabs.value = remaining
    if (!remaining.some((tab) => tab.path === route.fullPath)) {
        await router.replace(targetPath)
    }
}

async function closeAllTabs() {
    openTabs.value = []

    if (route.fullPath !== '/') {
        await router.replace('/')
    }
}

async function handleTabContextClick(tabPath: string, event: MenuClickEvent) {
    if (event.key === 'close-current') {
        await closeTab(tabPath)
        return
    }
    if (event.key === 'close-left') {
        await closeLeftTabs(tabPath)
        return
    }
    if (event.key === 'close-other') {
        await closeOtherTabs(tabPath)
        return
    }
    if (event.key === 'close-right') {
        await closeRightTabs(tabPath)
        return
    }
    if (event.key === 'close-all') {
        await closeAllTabs()
    }
}

function openMenuSearch() {
    menuSearchValue.value = ''
    menuSearchOpen.value = true
    void nextTick(() => menuSearchRef.value?.focus?.())
}

function selectFirstMenuSearchOption() {
    const firstOption = filteredMenuSearchOptions.value[0]
    if (firstOption) {
        void handleMenuSearch(firstOption.value)
    }
}

async function handleMenuSearch(path: string) {
    await router.push(path)
    expandActiveNavigationGroup()
    menuSearchValue.value = ''
    menuSearchOpen.value = false
    closeMobileNavigation()
}

async function handleAccountMenuClick(event: MenuClickEvent) {
    if (event.key === 'profile') {
        await router.push('/profile')
        return
    }
    if (event.key === 'logout') {
        logout()
    }
}

function handleGlobalShortcut(event: globalThis.KeyboardEvent) {
    if (
        event.key.toLowerCase() !== 'k' ||
        (!event.metaKey && !event.ctrlKey) ||
        event.altKey ||
        event.shiftKey
    ) {
        return
    }

    const target = event.target
    if (
        target instanceof globalThis.HTMLElement &&
        (target.isContentEditable ||
            target.tagName === 'INPUT' ||
            target.tagName === 'TEXTAREA')
    ) {
        return
    }

    event.preventDefault()
    openMenuSearch()
}

function resetContentScroll() {
    const target = appContentRef.value
    const element =
        target && 'scrollTop' in target && 'scrollLeft' in target
            ? target
            : target?.$el
    if (!element) return
    element.scrollLeft = 0
    element.scrollTop = 0
}

function logout() {
    Modal.confirm({
        title: '确认退出登录？',
        content: '退出后需要重新登录才能继续使用管理端。',
        okText: '退出登录',
        okType: 'danger',
        cancelText: '取消',
        onOk: performLogout,
    })
}

async function performLogout() {
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
    window.addEventListener('keydown', handleGlobalShortcut)
    void loadWatermark()
})

async function loadWatermark() {
    if (!authStore.getToken()) return
    try {
        const response = await systemSettingApi.publicSettings()
        const site = response.data.data.site as
            Record<string, unknown> | undefined
        siteInfo.value = {
            copyright: String(site?.copyright ?? '').trim(),
            icp: String(site?.icp ?? '').trim(),
        }
        watermark.value = {
            enabled: site?.watermarkEnabled === true,
            type: site?.watermarkType === 'username' ? 'username' : 'custom',
            content: String(site?.watermarkContent ?? ''),
            opacity: Math.min(
                1,
                Math.max(0, Number(site?.watermarkOpacity ?? 0.12)),
            ),
        }
    } catch {
        siteInfo.value = { copyright: '', icp: '' }
        watermark.value.enabled = false
    }
}

const watermarkText = computed(() =>
    watermark.value.type === 'username'
        ? displayName.value
        : watermark.value.content,
)

onBeforeUnmount(() => {
    window.removeEventListener('resize', updateViewport)
    window.removeEventListener('keydown', handleGlobalShortcut)
})

watch(
    () => [route.fullPath, route.path, route.meta.title, navigation.value],
    addCurrentTab,
    { immediate: true },
)

watch(() => [route.path, navigation.value], expandActiveNavigationGroup, {
    immediate: true,
})

watch(() => route.fullPath, resetContentScroll)
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
                <template v-for="item in visibleNavigation" :key="item.key">
                    <div
                        v-if="item.children && !sidebarCollapsed"
                        class="navigation-group"
                        :class="{
                            'navigation-group-expanded':
                                isNavigationGroupExpanded(item.key),
                        }"
                    >
                        <button
                            type="button"
                            class="navigation-group-title"
                            :class="{
                                'navigation-group-title-active': hasActiveChild(
                                    item.children,
                                ),
                            }"
                            :aria-expanded="isNavigationGroupExpanded(item.key)"
                            :aria-label="
                                isNavigationGroupExpanded(item.key)
                                    ? `收起${item.title}`
                                    : `展开${item.title}`
                            "
                            @click="toggleNavigationGroup(item.key)"
                        >
                            <component :is="item.icon" />
                            <span>{{ item.title }}</span>
                            <DownOutlined
                                v-if="isNavigationGroupExpanded(item.key)"
                                class="navigation-group-chevron"
                            />
                            <RightOutlined
                                v-else
                                class="navigation-group-chevron"
                            />
                        </button>
                        <template v-if="isNavigationGroupExpanded(item.key)">
                            <RouterLink
                                v-for="child in item.children"
                                :key="child.key"
                                :to="child.path ?? '/'"
                                class="navigation-link navigation-child-link"
                                :class="{
                                    'navigation-link-active':
                                        child.path && isActivePath(child.path),
                                }"
                                active-class=""
                                ><component :is="child.icon" /><span>{{
                                    child.title
                                }}</span></RouterLink
                            >
                        </template>
                    </div>
                    <RouterLink
                        v-else-if="item.path"
                        :to="item.path"
                        class="navigation-link"
                        :class="{
                            'navigation-link-active': isActivePath(item.path),
                        }"
                        active-class=""
                        :title="sidebarCollapsed ? item.title : undefined"
                        ><component :is="item.icon" /><span
                            v-if="!sidebarCollapsed"
                            >{{ item.title }}</span
                        ></RouterLink
                    >
                </template>
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
                <template v-for="item in navigation" :key="item.key">
                    <div
                        v-if="item.children"
                        class="navigation-group"
                        :class="{
                            'navigation-group-expanded':
                                isNavigationGroupExpanded(item.key),
                        }"
                    >
                        <button
                            type="button"
                            class="navigation-group-title"
                            :class="{
                                'navigation-group-title-active': hasActiveChild(
                                    item.children,
                                ),
                            }"
                            :aria-expanded="isNavigationGroupExpanded(item.key)"
                            :aria-label="
                                isNavigationGroupExpanded(item.key)
                                    ? `收起${item.title}`
                                    : `展开${item.title}`
                            "
                            @click="toggleNavigationGroup(item.key)"
                        >
                            <component :is="item.icon" />
                            <span>{{ item.title }}</span>
                            <DownOutlined
                                v-if="isNavigationGroupExpanded(item.key)"
                                class="navigation-group-chevron"
                            />
                            <RightOutlined
                                v-else
                                class="navigation-group-chevron"
                            />
                        </button>
                        <template v-if="isNavigationGroupExpanded(item.key)">
                            <RouterLink
                                v-for="child in item.children"
                                :key="child.key"
                                :to="child.path ?? '/'"
                                class="navigation-link navigation-child-link"
                                :class="{
                                    'navigation-link-active':
                                        child.path && isActivePath(child.path),
                                }"
                                active-class=""
                                @click="closeMobileNavigation"
                                ><component :is="child.icon" /><span>{{
                                    child.title
                                }}</span></RouterLink
                            >
                        </template>
                    </div>
                    <RouterLink
                        v-else-if="item.path"
                        :to="item.path"
                        class="navigation-link"
                        :class="{
                            'navigation-link-active': isActivePath(item.path),
                        }"
                        active-class=""
                        @click="closeMobileNavigation"
                        ><component :is="item.icon" /><span>{{
                            item.title
                        }}</span></RouterLink
                    >
                </template>
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
                <AppBreadcrumb :navigation="navigation" />
                <span class="header-spacer" />
                <a-button
                    type="text"
                    class="header-menu-search-trigger"
                    aria-label="打开快捷导航"
                    title="快捷导航（⌘/Ctrl + K）"
                    @click="openMenuSearch"
                    ><SearchOutlined
                /></a-button>
                <a-dropdown :trigger="['click']">
                    <a-button
                        type="text"
                        class="header-account-trigger"
                        aria-label="打开账户菜单"
                        title="账户菜单"
                    >
                        <a-avatar
                            :size="30"
                            :src="authStore.state.profile?.avatar"
                            class="header-avatar"
                            >{{ avatarText }}</a-avatar
                        >
                        <span class="header-user">{{ displayName }}</span>
                    </a-button>
                    <template #overlay>
                        <a-menu @click="handleAccountMenuClick">
                            <a-menu-item key="profile">
                                <span class="account-menu-label">
                                    <UserOutlined />
                                    <span>个人中心</span>
                                </span>
                            </a-menu-item>
                            <a-menu-divider />
                            <a-menu-item
                                key="logout"
                                data-testid="account-logout"
                            >
                                <span class="account-menu-label">
                                    <LogoutOutlined />
                                    <span>退出登录</span>
                                </span>
                            </a-menu-item>
                        </a-menu>
                    </template>
                </a-dropdown>
            </a-layout-header>
            <a-modal
                v-model:open="menuSearchOpen"
                class="command-menu-modal"
                title="快捷导航"
                :footer="null"
                :width="520"
                @cancel="menuSearchValue = ''"
            >
                <a-input
                    ref="menuSearchRef"
                    v-model:value="menuSearchValue"
                    class="command-menu-input"
                    placeholder="搜索可访问的菜单"
                    @press-enter="selectFirstMenuSearchOption"
                >
                    <template #prefix><SearchOutlined /></template>
                </a-input>
                <a-list
                    class="command-menu-list"
                    :data-source="filteredMenuSearchOptions"
                    :locale="{ emptyText: '没有匹配的菜单' }"
                >
                    <template #renderItem="{ item }">
                        <a-list-item
                            class="command-menu-option"
                            tabindex="0"
                            role="button"
                            @click="handleMenuSearch(item.value)"
                            @keydown.enter="handleMenuSearch(item.value)"
                        >
                            {{ item.label }}
                        </a-list-item>
                    </template>
                </a-list>
                <p class="command-menu-hint">按 Enter 打开第一项</p>
            </a-modal>
            <div class="page-tabs" aria-label="已打开页面">
                <a-dropdown
                    v-for="tab in openTabs"
                    :key="tab.path"
                    :trigger="['contextmenu']"
                >
                    <div
                        class="page-tab"
                        :class="{ 'page-tab-active': isActiveTab(tab.path) }"
                    >
                        <RouterLink
                            :to="tab.path"
                            class="page-tab-link"
                            active-class=""
                            >{{ tab.title }}</RouterLink
                        >
                        <a-button
                            type="text"
                            class="page-tab-close"
                            :aria-label="`关闭${tab.title}`"
                            :title="`关闭${tab.title}`"
                            @click.prevent.stop="closeTab(tab.path)"
                            ><CloseOutlined
                        /></a-button>
                    </div>
                    <template #overlay>
                        <a-menu
                            @click="
                                (event) =>
                                    handleTabContextClick(tab.path, event)
                            "
                        >
                            <a-menu-item key="close-current"
                                >关闭当前</a-menu-item
                            >
                            <a-menu-item key="close-left">关闭左侧</a-menu-item>
                            <a-menu-item key="close-right"
                                >关闭右侧</a-menu-item
                            >
                            <a-menu-item key="close-other"
                                >关闭其他</a-menu-item
                            >
                            <a-menu-item key="close-all">关闭全部</a-menu-item>
                        </a-menu>
                    </template>
                </a-dropdown>
            </div>
            <a-layout-content ref="appContentRef" class="app-content">
                <RouterView />
            </a-layout-content>
            <a-layout-footer
                v-if="siteInfo.copyright || siteInfo.icp"
                class="app-footer"
            >
                <span v-if="siteInfo.copyright">{{ siteInfo.copyright }}</span>
                <a-divider
                    v-if="siteInfo.copyright && siteInfo.icp"
                    type="vertical"
                />
                <span v-if="siteInfo.icp">{{ siteInfo.icp }}</span>
            </a-layout-footer>
        </a-layout>
        <div
            v-if="watermark.enabled && watermarkText"
            class="global-watermark"
            :style="{ opacity: watermark.opacity }"
            aria-hidden="true"
        >
            <span v-for="index in 36" :key="index">{{ watermarkText }}</span>
        </div>
    </a-layout>
</template>

<style scoped>
.global-watermark {
    position: fixed;
    inset: 0;
    z-index: 20;
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    align-content: space-around;
    justify-items: center;
    column-gap: 96px;
    row-gap: 72px;
    pointer-events: none;
    overflow: hidden;
    color: #1f2937;
    font-size: 16px;
    transform: rotate(-24deg) scale(1.2);
}
.app-footer {
    padding: 12px 24px 20px;
    color: var(--alpha-text-secondary);
    font-size: 12px;
    text-align: center;
}
.global-watermark span {
    white-space: nowrap;
}

@media (max-width: 767px) {
    .global-watermark {
        grid-template-columns: repeat(3, 1fr);
        column-gap: 56px;
        row-gap: 48px;
        font-size: 14px;
    }
}
</style>
