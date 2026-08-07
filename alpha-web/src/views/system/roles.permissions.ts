export interface RolePermissionMenu {
    id: number
    parentId?: number
    title: string
    menuType: 'DIRECTORY' | 'MENU' | 'BUTTON'
    permission?: string
}

export interface RolePermissionTreeNode {
    key: number
    title: string
    menuType: RolePermissionMenu['menuType']
    permission?: string
    children?: RolePermissionTreeNode[]
}

export function buildRolePermissionTree(
    menus: RolePermissionMenu[],
): RolePermissionTreeNode[] {
    const nodes = new Map<number, RolePermissionTreeNode>()
    const children = new Map<number, RolePermissionTreeNode[]>()

    for (const menu of menus) {
        nodes.set(menu.id, {
            key: menu.id,
            title: menu.title,
            menuType: menu.menuType,
            permission: menu.permission,
        })
    }
    for (const menu of menus) {
        if (menu.parentId && nodes.has(menu.parentId)) {
            const siblings = children.get(menu.parentId) ?? []
            siblings.push(nodes.get(menu.id)!)
            children.set(menu.parentId, siblings)
        }
    }
    for (const [id, descendants] of children) {
        nodes.get(id)!.children = descendants
    }
    return menus
        .filter((menu) => !menu.parentId || !nodes.has(menu.parentId))
        .map((menu) => nodes.get(menu.id)!)
}

export function collectRolePermissionKeys(
    nodes: RolePermissionTreeNode[],
): number[] {
    return nodes.flatMap((node) => [
        node.key,
        ...collectRolePermissionKeys(node.children ?? []),
    ])
}

/**
 * 为已选权限补齐所有祖先菜单，保证按钮权限始终拥有页面入口。
 */
export function withRolePermissionAncestors(
    menus: RolePermissionMenu[],
    selectedKeys: readonly (number | string)[],
): number[] {
    const menusById = new Map(menus.map((menu) => [menu.id, menu]))
    const selected = new Set(
        selectedKeys
            .map(Number)
            .filter((key): key is number => Number.isSafeInteger(key)),
    )

    for (const selectedKey of [...selected]) {
        let parentId = menusById.get(selectedKey)?.parentId
        while (parentId && menusById.has(parentId)) {
            selected.add(parentId)
            parentId = menusById.get(parentId)?.parentId
        }
    }

    return [...selected]
}

/**
 * 点击权限名称时切换节点，并同步处理其后代和祖先菜单。
 */
export function toggleRolePermission(
    menus: RolePermissionMenu[],
    selectedKeys: readonly (number | string)[],
    targetKey: number,
): number[] {
    const selected = new Set(selectedKeys)
    const isSelected = selected.has(targetKey)
    const descendants = new Set<number>()
    const collectDescendants = (parentId: number) => {
        menus
            .filter((menu) => menu.parentId === parentId)
            .forEach((menu) => {
                descendants.add(menu.id)
                collectDescendants(menu.id)
            })
    }

    if (isSelected) {
        selected.delete(targetKey)
        collectDescendants(targetKey)
        descendants.forEach((id) => selected.delete(id))
    } else {
        selected.add(targetKey)
    }

    return withRolePermissionAncestors(menus, [...selected])
}
