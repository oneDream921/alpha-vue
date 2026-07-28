import type { Component } from 'vue'
import {
    ApartmentOutlined,
    BookOutlined,
    DashboardOutlined,
    DatabaseOutlined,
    FileSearchOutlined,
    FileTextOutlined,
    FolderOpenOutlined,
    MenuOutlined,
    SafetyOutlined,
    SettingOutlined,
    UserOutlined,
} from '@ant-design/icons-vue'

import type { AppRoute } from '@/stores/auth'

export interface NavigationNode {
    key: string
    title: string
    icon: Component
    path?: string
    permission?: string
    children?: NavigationNode[]
}

export interface OpenTab {
    title: string
    path: string
}

interface NavigationMeta {
    componentId: string
    path: string
    title: string
    icon: Component
    permission?: string
}

const iconMap: Record<string, Component> = {
    ApartmentOutlined,
    BookOutlined,
    DashboardOutlined,
    DatabaseOutlined,
    FileSearchOutlined,
    FileTextOutlined,
    FolderOpenOutlined,
    MenuOutlined,
    SafetyOutlined,
    SettingOutlined,
    UserOutlined,
}

const dashboardNode: NavigationNode = {
    key: 'static:/',
    path: '/',
    title: '工作台',
    icon: DashboardOutlined,
}

const profileNode: NavigationNode = {
    key: 'static:/profile',
    path: '/profile',
    title: '个人中心',
    icon: UserOutlined,
}

const managedNavigationMeta = [
    {
        componentId: 'system/users',
        path: '/system/users',
        title: '用户管理',
        icon: UserOutlined,
        permission: 'system:user:list',
    },
    {
        componentId: 'system/roles',
        path: '/system/roles',
        title: '角色管理',
        icon: SafetyOutlined,
        permission: 'system:role:list',
    },
    {
        componentId: 'system/menus',
        path: '/system/menus',
        title: '菜单管理',
        icon: MenuOutlined,
        permission: 'system:menu:list',
    },
    {
        componentId: 'system/depts',
        path: '/system/depts',
        title: '部门管理',
        icon: ApartmentOutlined,
        permission: 'system:dept:list',
    },
    {
        componentId: 'system/configs',
        path: '/system/configs',
        title: '参数配置',
        icon: SettingOutlined,
        permission: 'system:config:list',
    },
    {
        componentId: 'system/dicts',
        path: '/system/dicts',
        title: '数据字典',
        icon: BookOutlined,
        permission: 'system:dict:list',
    },
    {
        componentId: 'monitor/redis',
        path: '/monitor/redis',
        title: 'Redis 管理',
        icon: DatabaseOutlined,
        permission: 'monitor:redis:list',
    },
    {
        componentId: 'monitor/sql',
        path: '/monitor/sql',
        title: 'SQL 日志',
        icon: FileSearchOutlined,
        permission: 'monitor:sql:list',
    },
    {
        componentId: 'files/index',
        path: '/files',
        title: '文件管理',
        icon: FolderOpenOutlined,
        permission: 'file:list',
    },
    {
        componentId: 'system/logs',
        path: '/logs',
        title: '审计日志',
        icon: FileTextOutlined,
        permission: 'log:operation:list',
    },
] as const satisfies readonly NavigationMeta[]

const metaByComponent: ReadonlyMap<string, NavigationMeta> = new Map(
    managedNavigationMeta.map((item) => [item.componentId, item]),
)

const fallbackBusinessNodes: NavigationNode[] = managedNavigationMeta.map(
    (item) => ({
        key: `fallback:${item.path}`,
        path: item.path,
        title: item.title,
        icon: item.icon,
        permission: item.permission,
    }),
)

function routeIcon(route: AppRoute, fallback: Component = MenuOutlined) {
    return route.icon ? (iconMap[route.icon] ?? fallback) : fallback
}

function hasAccess(
    node: Pick<NavigationNode, 'permission'>,
    hasPermission: (permission: string) => boolean,
) {
    return !node.permission || hasPermission(node.permission)
}

function routeSorter(left: AppRoute, right: AppRoute) {
    return left.sortOrder === right.sortOrder
        ? left.id - right.id
        : left.sortOrder - right.sortOrder
}

function buildRouteNode(
    route: AppRoute,
    childrenByParent: Map<number, AppRoute[]>,
    hasPermission: (permission: string) => boolean,
): NavigationNode | null {
    if (route.menuType === 'BUTTON') {
        return null
    }

    const children = (childrenByParent.get(route.id) ?? [])
        .map((child) => buildRouteNode(child, childrenByParent, hasPermission))
        .filter((item): item is NavigationNode => item !== null)
    const meta = route.component ? metaByComponent.get(route.component) : null

    if (children.length > 0) {
        return {
            key: `route:${route.id}`,
            title: meta?.title ?? route.title,
            icon: routeIcon(route, meta?.icon),
            children: flattenNodes(children),
        }
    }

    if (!meta) {
        return null
    }

    const node: NavigationNode = {
        key: `route:${route.id}`,
        path: meta.path,
        title: meta.title,
        icon: meta.icon,
        permission: meta.permission,
    }

    return hasAccess(node, hasPermission) ? node : null
}

function distinctNodes(nodes: readonly NavigationNode[]) {
    const seen = new Set<string>()
    return nodes.filter((node) => {
        const key = node.path ?? node.key
        if (seen.has(key)) {
            return false
        }
        seen.add(key)
        return true
    })
}

function flattenNodes(nodes: readonly NavigationNode[]): NavigationNode[] {
    return nodes.flatMap((node) =>
        node.children ? flattenNodes(node.children) : [node],
    )
}

export function buildNavigation(
    routes: readonly AppRoute[],
    hasPermission: (permission: string) => boolean,
) {
    const childrenByParent = new Map<number, AppRoute[]>()
    routes
        .filter((route) => route.menuType !== 'BUTTON')
        .forEach((route) => {
            const children = childrenByParent.get(route.parentId) ?? []
            children.push(route)
            childrenByParent.set(route.parentId, children)
        })
    childrenByParent.forEach((children) => children.sort(routeSorter))

    const dynamicNodes = (childrenByParent.get(0) ?? [])
        .map((route) => buildRouteNode(route, childrenByParent, hasPermission))
        .filter((item): item is NavigationNode => item !== null)
        .filter((item) => item.path !== '/')
    const businessNodes =
        dynamicNodes.length > 0
            ? dynamicNodes
            : fallbackBusinessNodes.filter((node) =>
                  hasAccess(node, hasPermission),
              )

    return [dashboardNode, ...businessNodes, profileNode]
}

export function flattenNavigationLeaves(nodes: readonly NavigationNode[]) {
    return distinctNodes(flattenNodes(nodes)).filter(
        (node): node is NavigationNode & { path: string } =>
            typeof node.path === 'string',
    )
}

export function tabTitleForPath(
    path: string,
    nodes: readonly NavigationNode[],
    fallbackTitle: string,
) {
    return (
        flattenNavigationLeaves(nodes).find((item) => item.path === path)
            ?.title ?? fallbackTitle
    )
}

export function menuTrailForPath(
    path: string,
    nodes: readonly NavigationNode[],
): NavigationNode[] {
    for (const node of nodes) {
        if (node.path === path) {
            return [node]
        }

        if (node.children) {
            const childTrail = menuTrailForPath(path, node.children)
            if (childTrail.length > 0) {
                return [node, ...childTrail]
            }
        }
    }

    return []
}

export function closeTabAt(
    tabs: readonly OpenTab[],
    targetPath: string,
): OpenTab[] {
    return tabs.filter((tab) => tab.path !== targetPath)
}

export function closeTabsLeftOf(
    tabs: readonly OpenTab[],
    targetPath: string,
): OpenTab[] {
    const targetIndex = tabs.findIndex((tab) => tab.path === targetPath)
    return targetIndex <= 0 ? [...tabs] : tabs.slice(targetIndex)
}

export function closeTabsExcept(
    tabs: readonly OpenTab[],
    targetPath: string,
): OpenTab[] {
    return tabs.filter((tab) => tab.path === targetPath)
}
