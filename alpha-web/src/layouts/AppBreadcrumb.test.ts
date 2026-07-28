import Antd from 'ant-design-vue'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppBreadcrumb from './AppBreadcrumb.vue'
import { buildNavigation } from './navigation'
import type { AppRoute } from '@/stores/auth'

function createTestRouter() {
    return createRouter({
        history: createMemoryHistory(),
        routes: [
            {
                path: '/',
                component: { template: '<div />' },
                meta: { title: '工作台' },
            },
            {
                path: '/system/users',
                component: { template: '<div />' },
                meta: { title: '用户管理' },
            },
            {
                path: '/system/roles',
                component: { template: '<div />' },
                meta: { title: '角色管理' },
            },
        ],
    })
}

const routes: AppRoute[] = [
    {
        id: 2,
        parentId: 0,
        title: '系统管理',
        menuType: 'MENU',
        path: '/system',
        component: 'Layout',
        icon: 'SettingOutlined',
        sortOrder: 2,
    },
    {
        id: 3,
        parentId: 2,
        title: 'Users',
        menuType: 'MENU',
        path: 'users',
        component: 'system/users',
        permission: 'system:user:list',
        icon: 'UserOutlined',
        sortOrder: 1,
    },
    {
        id: 4,
        parentId: 2,
        title: '角色管理',
        menuType: 'MENU',
        path: 'roles',
        component: 'system/roles',
        permission: 'system:role:list',
        icon: 'SafetyOutlined',
        sortOrder: 2,
    },
]

describe('AppBreadcrumb', () => {
    it('renders the current route menu hierarchy instead of visited pages', async () => {
        const router = createTestRouter()
        await router.push('/system/users')
        await router.isReady()

        const wrapper = mount(AppBreadcrumb, {
            props: {
                navigation: buildNavigation(routes, () => true),
            },
            global: {
                plugins: [router, Antd],
            },
        })

        await router.push('/system/roles')
        await nextTick()

        expect(wrapper.text()).toContain('系统管理')
        expect(wrapper.text()).toContain('角色管理')
        expect(wrapper.text()).not.toContain('用户管理')
        expect(wrapper.findAll('.app-breadcrumb a')).toHaveLength(0)
        expect(wrapper.find('.breadcrumb-current').text()).toBe('角色管理')
    })
})
