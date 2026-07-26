import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHistory } from 'vue-router'

import BaseLayout from '@/layouts/BaseLayout.vue'
import type { AppRoute } from '@/stores/auth'
import { authGuard } from './guard'

type ManagedRouteDefinition = RouteRecordRaw & { componentId: string }

const managementRouteDefinitions = [
    {
        path: 'system/users',
        name: 'users',
        componentId: 'system/users',
        component: () => import('@/views/system/users.vue'),
        meta: { permission: 'system:user:list' },
    },
    {
        path: 'system/roles',
        name: 'roles',
        componentId: 'system/roles',
        component: () => import('@/views/system/roles.vue'),
        meta: { permission: 'system:role:list' },
    },
    {
        path: 'system/menus',
        name: 'menus',
        componentId: 'system/menus',
        component: () => import('@/views/system/menus.vue'),
        meta: { permission: 'system:menu:list' },
    },
    {
        path: 'system/depts',
        name: 'depts',
        componentId: 'system/depts',
        component: () => import('@/views/system/depts.vue'),
        meta: { permission: 'system:dept:list' },
    },
    {
        path: 'system/configs',
        name: 'configs',
        componentId: 'system/configs',
        component: () => import('@/views/system/configs.vue'),
        meta: { permission: 'system:config:list' },
    },
    {
        path: 'system/dicts',
        name: 'dicts',
        componentId: 'system/dicts',
        component: () => import('@/views/system/dicts.vue'),
        meta: { permission: 'system:dict:list' },
    },
    {
        path: 'monitor/redis',
        name: 'redis',
        componentId: 'monitor/redis',
        component: () => import('@/views/monitor/redis.vue'),
        meta: { permission: 'monitor:redis:list' },
    },
    {
        path: 'files',
        name: 'files',
        componentId: 'files/index',
        component: () => import('@/views/files/index.vue'),
        meta: { permission: 'file:list' },
    },
    {
        path: 'logs',
        name: 'logs',
        componentId: 'system/logs',
        component: () => import('@/views/system/logs.vue'),
        meta: { permission: 'log:operation:list' },
    },
] as const satisfies readonly ManagedRouteDefinition[]

const activeManagementRouteNames = new Set<string>()

/**
 * 将后端菜单映射为受控的前端组件白名单，拒绝数据库中未知的组件标识。
 */
export function managementRoutesFor(routes: readonly AppRoute[]) {
    return managementRouteDefinitions.filter((definition) => {
        const permission = definition.meta?.permission as string
        const component = definition.componentId
        return routes.some(
            (route) =>
                route.menuType === 'MENU' &&
                route.component === component &&
                route.permission === permission,
        )
    })
}

/**
 * 按当前会话可见菜单注册业务路由。
 */
export function ensureManagementRoutes(routes: readonly AppRoute[]) {
    clearManagementRoutes()
    managementRoutesFor(routes).forEach((route) => {
        router.addRoute('app', route)
        activeManagementRouteNames.add(route.name as string)
    })
}

/**
 * 清除会话相关的动态业务路由。
 */
export function clearManagementRoutes() {
    activeManagementRouteNames.forEach((name) => router.removeRoute(name))
    activeManagementRouteNames.clear()
}

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: () => import('@/views/login/index.vue'),
            meta: { requiresAuth: false },
        },
        {
            path: '/',
            name: 'app',
            component: BaseLayout,
            children: [
                {
                    path: '',
                    name: 'dashboard',
                    component: () => import('@/views/home/index.vue'),
                },
                {
                    path: 'profile',
                    name: 'profile',
                    component: () => import('@/views/profile/index.vue'),
                },
            ],
        },
        {
            path: '/403',
            name: 'forbidden',
            component: () => import('@/views/errors/403.vue'),
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('@/views/errors/404.vue'),
            meta: { requiresAuth: false },
        },
    ],
})

router.beforeEach(authGuard)

export default router
