import Antd from 'ant-design-vue'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

const routeMock = vi.hoisted(() => ({
    path: '/',
    fullPath: '/',
    meta: { title: '工作台' } as Record<string, unknown>,
}))

vi.mock('vue-router', () => ({
    createRouter: () => ({
        addRoute: vi.fn(),
        beforeEach: vi.fn(),
        removeRoute: vi.fn(),
    }),
    createWebHistory: vi.fn(),
    useRoute: () => routeMock,
    useRouter: () => ({ replace: vi.fn() }),
}))

import BaseLayout from './BaseLayout.vue'
import { authStore } from '@/stores/auth'

describe('BaseLayout', () => {
    afterEach(() => {
        authStore.clearAuth()
        routeMock.path = '/'
        routeMock.fullPath = '/'
        routeMock.meta = { title: '工作台' }
        window.innerWidth = 1024
        window.dispatchEvent(new Event('resize'))
    })

    it('renders drawer navigation below the mobile breakpoint', async () => {
        window.innerWidth = 767
        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        await wrapper.get('[data-testid="mobile-nav-trigger"]').trigger('click')

        expect(
            document.body.querySelector('[data-testid="mobile-navigation"]'),
        ).not.toBeNull()
        expect(document.body.textContent).toContain('工作台')
    })

    it('starts collapsed on tablet and expands the sidebar through an accessible control', async () => {
        window.innerWidth = 768
        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        const sidebar = wrapper.get('.desktop-sidebar')
        const toggle = wrapper.get('[aria-label="展开侧栏"]')

        expect(sidebar.classes()).toContain('ant-layout-sider-collapsed')

        await toggle.trigger('click')

        expect(sidebar.classes()).not.toContain('ant-layout-sider-collapsed')
        expect(wrapper.get('[aria-label="折叠侧栏"]')).toBeTruthy()
    })

    it('collapses and expands the desktop sidebar through its toggle', async () => {
        window.innerWidth = 1024
        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        const sidebar = wrapper.get('.desktop-sidebar')

        expect(sidebar.classes()).not.toContain('ant-layout-sider-collapsed')

        await wrapper.get('[aria-label="折叠侧栏"]').trigger('click')

        expect(sidebar.classes()).toContain('ant-layout-sider-collapsed')

        await wrapper.get('[aria-label="展开侧栏"]').trigger('click')

        expect(sidebar.classes()).not.toContain('ant-layout-sider-collapsed')
        expect(wrapper.get('[aria-label="折叠侧栏"]')).toBeTruthy()
    })

    it('uses the fixed Chinese navigation labels instead of route-title fallbacks', () => {
        authStore.setSession(
            'test-token',
            {
                id: 1,
                username: 'admin',
                roles: ['SUPER_ADMIN'],
                permissions: ['*'],
                mustChangePassword: false,
            },
            [
                {
                    id: 3,
                    parentId: 2,
                    title: 'Users',
                    menuType: 'MENU',
                    path: 'users',
                    sortOrder: 1,
                },
            ],
        )

        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        expect(wrapper.text()).toContain('用户管理')
        expect(wrapper.text()).not.toContain('Users')
    })

    it('renders visited breadcrumb entries with the current page as the last item', () => {
        routeMock.path = '/system/users'
        routeMock.fullPath = '/system/users'
        routeMock.meta = {
            title: '用户管理',
        }

        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        const breadcrumb = wrapper.get('.app-breadcrumb')
        const currentItems = wrapper.findAll('.breadcrumb-current')

        expect(breadcrumb.text()).toContain('工作台')
        expect(breadcrumb.text()).toContain('用户管理')
        expect(wrapper.get('.app-breadcrumb a').text()).toBe('工作台')
        expect(currentItems[currentItems.length - 1].text()).toBe('用户管理')
    })

    it('shows parameter configuration only to users with its list permission', () => {
        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:user:list'],
            mustChangePassword: false,
        })

        const withoutPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withoutPermission.text()).not.toContain('参数配置')
        withoutPermission.unmount()

        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:config:list'],
            mustChangePassword: false,
        })
        const withPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withPermission.text()).toContain('参数配置')
    })

    it('shows data dictionary only to users with its list permission', () => {
        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:user:list'],
            mustChangePassword: false,
        })
        const withoutPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withoutPermission.text()).not.toContain('数据字典')
        withoutPermission.unmount()

        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:dict:list'],
            mustChangePassword: false,
        })
        const withPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withPermission.text()).toContain('数据字典')
    })

    it('shows Redis management only to users with its list permission', () => {
        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:user:list'],
            mustChangePassword: false,
        })
        const withoutPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withoutPermission.text()).not.toContain('Redis 管理')
        withoutPermission.unmount()

        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['monitor:redis:list'],
            mustChangePassword: false,
        })
        const withPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withPermission.text()).toContain('Redis 管理')
    })

    it('shows SQL logs only to users with its list permission', () => {
        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['monitor:redis:list'],
            mustChangePassword: false,
        })
        const withoutPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withoutPermission.text()).not.toContain('SQL 日志')
        withoutPermission.unmount()

        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['monitor:sql:list'],
            mustChangePassword: false,
        })
        const withPermission = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })
        expect(withPermission.text()).toContain('SQL 日志')
    })
})
