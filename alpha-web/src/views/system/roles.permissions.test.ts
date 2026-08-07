import { describe, expect, it } from 'vitest'

import {
    buildRolePermissionTree,
    collectRolePermissionKeys,
    toggleRolePermission,
    withRolePermissionAncestors,
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

    it('adds every ancestor when a button permission is selected', () => {
        expect(withRolePermissionAncestors(menus, [3])).toEqual([3, 2, 1])
        expect(withRolePermissionAncestors(menus, ['3'])).toEqual([3, 2, 1])
    })

    it('keeps the selected menu set unchanged when the page menu is selected', () => {
        expect(withRolePermissionAncestors(menus, [2])).toEqual([2, 1])
    })

    it('toggles a permission title and removes descendants with its parent', () => {
        expect(toggleRolePermission(menus, [1, 2, 3], 2)).toEqual([1])
        expect(toggleRolePermission(menus, [1], 2)).toEqual([1, 2])
    })
})
