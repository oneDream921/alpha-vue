import { describe, expect, it } from 'vitest'

import router, { managementRoutesFor } from './index'

describe('managementRoutesFor', () => {
    it('registers managed routes so direct links reach the auth guard', () => {
        expect(router.hasRoute('redis')).toBe(true)
        expect(router.hasRoute('sql-monitor')).toBe(true)
    })

    it('returns only routes granted by the profile permissions', () => {
        const routes = managementRoutesFor([
            {
                id: 3,
                parentId: 2,
                title: '用户管理',
                menuType: 'MENU',
                path: '/system/users',
                component: 'system/users',
                permission: 'system:user:list',
                sortOrder: 1,
            },
            {
                id: 7,
                parentId: 0,
                title: '文件管理',
                menuType: 'MENU',
                path: '/files',
                component: 'files/index',
                permission: 'file:list',
                sortOrder: 2,
            },
            {
                id: 50,
                parentId: 2,
                title: '系统配置',
                menuType: 'MENU',
                path: 'settings',
                component: 'system/settings',
                permission: 'system:setting:list',
                sortOrder: 3,
            },
            {
                id: 31,
                parentId: 2,
                title: '数据字典',
                menuType: 'MENU',
                path: 'dicts',
                component: 'system/dicts',
                permission: 'system:dict:list',
                sortOrder: 4,
            },
            {
                id: 35,
                parentId: 0,
                title: 'Redis 管理',
                menuType: 'MENU',
                path: '/monitor/redis',
                component: 'monitor/redis',
                permission: 'monitor:redis:list',
                sortOrder: 5,
            },
            {
                id: 37,
                parentId: 0,
                title: 'SQL 日志',
                menuType: 'MENU',
                path: '/monitor/sql',
                component: 'monitor/sql',
                permission: 'monitor:sql:list',
                sortOrder: 6,
            },
            {
                id: 99,
                parentId: 0,
                title: '未知组件',
                menuType: 'MENU',
                component: 'external/untrusted',
                permission: 'external:read',
                sortOrder: 3,
            },
        ])

        expect(routes.map((route) => route.name)).toEqual([
            'users',
            'settings',
            'dicts',
            'redis',
            'sql-monitor',
            'files',
        ])
    })
})
