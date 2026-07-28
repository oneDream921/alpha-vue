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
