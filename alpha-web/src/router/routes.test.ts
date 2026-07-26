import { describe, expect, it } from 'vitest'

import { managementRoutesFor } from './index'

describe('managementRoutesFor', () => {
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
                id: 27,
                parentId: 2,
                title: '参数配置',
                menuType: 'MENU',
                path: 'configs',
                component: 'system/configs',
                permission: 'system:config:list',
                sortOrder: 3,
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
            'configs',
            'files',
        ])
    })
})
