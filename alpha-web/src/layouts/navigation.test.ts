import { describe, expect, it } from 'vitest'

import {
    buildNavigation,
    closeTabAt,
    closeTabsExcept,
    closeTabsLeftOf,
    flattenNavigationLeaves,
    menuTrailForPath,
    tabTitleForPath,
    type OpenTab,
} from './navigation'
import type { AppRoute } from '@/stores/auth'

const allowAll = () => true

describe('layout navigation', () => {
    it('builds backend menu routes into a visible hierarchy', () => {
        const routes: AppRoute[] = [
            {
                id: 2,
                parentId: 0,
                title: '系统管理',
                menuType: 'MENU',
                path: '/system',
                component: 'Layout',
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
                sortOrder: 2,
            },
            {
                id: 99,
                parentId: 2,
                title: '未知页面',
                menuType: 'MENU',
                component: 'external/untrusted',
                permission: 'external:list',
                sortOrder: 3,
            },
        ]

        const navigation = buildNavigation(routes, allowAll)
        const system = navigation.find((item) => item.title === '系统管理')

        expect(system?.children?.map((item) => item.title)).toEqual([
            '用户管理',
            '角色管理',
        ])
        expect(
            flattenNavigationLeaves(navigation).map((item) => item.path),
        ).toContain('/system/users')
        expect(
            menuTrailForPath('/system/users', navigation).map(
                (item) => item.title,
            ),
        ).toEqual(['系统管理', '用户管理'])
        expect(tabTitleForPath('/system/users', navigation, 'Users')).toBe(
            '用户管理',
        )
    })

    it('falls back to the managed whitelist when backend routes are empty', () => {
        const navigation = buildNavigation(
            [],
            (permission) => permission === 'system:config:list',
        )

        expect(
            flattenNavigationLeaves(navigation).map((item) => item.title),
        ).toEqual(['工作台', '参数配置', '个人中心'])
    })

    it('computes tab close ranges without mutating the input list', () => {
        const tabs: OpenTab[] = [
            { path: '/', title: '工作台' },
            { path: '/system/users', title: '用户管理' },
            { path: '/system/roles', title: '角色管理' },
        ]

        expect(
            closeTabAt(tabs, '/system/users').map((tab) => tab.path),
        ).toEqual(['/', '/system/roles'])
        expect(
            closeTabsLeftOf(tabs, '/system/roles').map((tab) => tab.path),
        ).toEqual(['/system/roles'])
        expect(closeTabsExcept(tabs, '/system/users')).toEqual([
            { path: '/system/users', title: '用户管理' },
        ])
        expect(tabs.map((tab) => tab.path)).toEqual([
            '/',
            '/system/users',
            '/system/roles',
        ])
    })
})
