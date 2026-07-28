<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { menuTrailForPath, type NavigationNode } from './navigation'

interface BreadcrumbItem {
    key: string
    title: string
    path?: string
}

const route = useRoute()

const props = defineProps<{
    navigation: NavigationNode[]
}>()

function currentRouteTitle() {
    return typeof route.meta.title === 'string'
        ? route.meta.title
        : '管理控制台'
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
    if (route.meta.breadcrumb === false) {
        return []
    }

    const menuTrail = menuTrailForPath(route.path, props.navigation)
    return menuTrail.length > 0
        ? menuTrail.map((item) => ({
              key: item.key,
              title: item.title,
              path: item.path,
          }))
        : [
              {
                  key: `route:${route.path}`,
                  title: currentRouteTitle(),
              },
          ]
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
            <span
                v-else
                :class="{
                    'breadcrumb-current': index === breadcrumbs.length - 1,
                    'breadcrumb-parent': index < breadcrumbs.length - 1,
                }"
                >{{ item.title }}</span
            >
        </a-breadcrumb-item>
    </a-breadcrumb>
</template>
