<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

interface BreadcrumbItem {
    title: string
    path: string
}

const dashboardItem: BreadcrumbItem = { title: '工作台', path: '/' }
const maxBreadcrumbItems = 6
const route = useRoute()
const visitedItems = ref<BreadcrumbItem[]>([])

function currentRouteTitle() {
    return typeof route.meta.title === 'string'
        ? route.meta.title
        : '管理控制台'
}

function currentRoutePath() {
    return typeof route.fullPath === 'string' && route.fullPath.length > 0
        ? route.fullPath
        : route.path
}

function rememberCurrentRoute() {
    if (route.meta.breadcrumb === false) {
        return
    }

    const currentItem: BreadcrumbItem = {
        title: currentRouteTitle(),
        path: currentRoutePath(),
    }
    const previousItems = visitedItems.value.filter(
        (item) => item.path !== currentItem.path,
    )

    visitedItems.value = [...previousItems, currentItem].slice(
        -maxBreadcrumbItems,
    )
}

watch(
    () => [route.fullPath, route.path, route.meta.title, route.meta.breadcrumb],
    rememberCurrentRoute,
    { immediate: true },
)

const breadcrumbs = computed(() => {
    if (visitedItems.value.some((item) => item.path === dashboardItem.path)) {
        return visitedItems.value
    }
    return [dashboardItem, ...visitedItems.value]
})
</script>

<template>
    <a-breadcrumb
        v-if="breadcrumbs.length > 0"
        class="app-breadcrumb"
        separator="/"
    >
        <a-breadcrumb-item
            v-for="(item, index) in breadcrumbs"
            :key="`${item.path || item.title}-${index}`"
        >
            <RouterLink
                v-if="item.path && index < breadcrumbs.length - 1"
                :to="item.path"
                >{{ item.title }}</RouterLink
            >
            <span v-else class="breadcrumb-current">{{ item.title }}</span>
        </a-breadcrumb-item>
    </a-breadcrumb>
</template>
