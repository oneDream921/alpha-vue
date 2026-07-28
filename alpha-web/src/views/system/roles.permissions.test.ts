import { describe, expect, it } from 'vitest'

import {
    buildRolePermissionTree,
    collectRolePermissionKeys,
    type RolePermissionMenu,
} from './roles.permissions'

const menus: RolePermissionMenu[] = [
    {
        id: 1,
        title: '系统管理',
        menuType: 'DIRECTORY',
    },
    {
        id: 2,
        parentId: 1,
        title: '角色管理',
        menuType: 'MENU',
    },
    {
        id: 3,
        parentId: 2,
        title: '分配权限',
        menuType: 'BUTTON',
        permission: 'system:role:assign',
    },
]

describe('role permission tree', () => {
    it('keeps menu hierarchy and every permission key', () => {
        const tree = buildRolePermissionTree(menus)

        expect(tree[0].children?.[0].children?.[0].title).toBe('分配权限')
        expect(collectRolePermissionKeys(tree)).toEqual([1, 2, 3])
    })
})
