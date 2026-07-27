import Antd from 'ant-design-vue'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppBreadcrumb from './AppBreadcrumb.vue'

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

describe('AppBreadcrumb', () => {
    it('keeps visited pages in navigation order and leaves the current page inactive', async () => {
        const router = createTestRouter()
        await router.push('/')
        await router.isReady()

        const wrapper = mount(AppBreadcrumb, {
            global: {
                plugins: [router, Antd],
            },
        })

        await router.push('/system/users')
        await nextTick()
        await router.push('/system/roles')
        await nextTick()

        expect(wrapper.text()).toContain('工作台')
        expect(wrapper.text()).toContain('用户管理')
        expect(wrapper.text()).toContain('角色管理')
        expect(
            wrapper.findAll('.app-breadcrumb a').map((item) => item.text()),
        ).toEqual(['工作台', '用户管理'])
        expect(wrapper.find('.breadcrumb-current').text()).toBe('角色管理')
    })
})
