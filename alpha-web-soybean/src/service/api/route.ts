import type { ElegantConstRoute, LastLevelRouteKey } from '@elegant-router/types';
import type { AppRoute } from '@/stores/auth';
import { request } from '../request';

interface RouteDefinition {
  name: string;
  path: string;
  component: string;
  title: string;
  permission: string;
  icon: string;
  parent?: string;
}

const routeDefinitions: RouteDefinition[] = [
  {
    name: 'system_users',
    path: '/system/users',
    component: 'system-users',
    title: '用户管理',
    permission: 'system:user:list',
    icon: 'ant-design:user-outlined',
    parent: 'system'
  },
  {
    name: 'system_roles',
    path: '/system/roles',
    component: 'system-roles',
    title: '角色管理',
    permission: 'system:role:list',
    icon: 'ant-design:team-outlined',
    parent: 'system'
  },
  {
    name: 'system_menus',
    path: '/system/menus',
    component: 'system-menus',
    title: '菜单管理',
    permission: 'system:menu:list',
    icon: 'ant-design:menu-outlined',
    parent: 'system'
  },
  {
    name: 'system_depts',
    path: '/system/depts',
    component: 'system-depts',
    title: '部门管理',
    permission: 'system:dept:list',
    icon: 'ant-design:apartment-outlined',
    parent: 'system'
  },
  {
    name: 'system_settings',
    path: '/system/settings',
    component: 'system-settings',
    title: '系统配置',
    permission: 'system:setting:list',
    icon: 'ant-design:setting-outlined',
    parent: 'system'
  },
  {
    name: 'system_dicts',
    path: '/system/dicts',
    component: 'system-dicts',
    title: '数据字典',
    permission: 'system:dict:list',
    icon: 'ant-design:book-outlined',
    parent: 'system'
  },
  {
    name: 'monitor_redis',
    path: '/monitor/redis',
    component: 'monitor-redis',
    title: 'Redis 管理',
    permission: 'monitor:redis:list',
    icon: 'ant-design:database-outlined',
    parent: 'monitor'
  },
  {
    name: 'monitor_sql',
    path: '/monitor/sql',
    component: 'monitor-sql',
    title: 'SQL 日志',
    permission: 'monitor:sql:list',
    icon: 'ant-design:code-outlined',
    parent: 'monitor'
  },
  {
    name: 'monitor_online-users',
    path: '/monitor/online-users',
    component: 'monitor-online-users',
    title: '在线用户',
    permission: 'monitor:online:list',
    icon: 'ant-design:cloud-server-outlined',
    parent: 'monitor'
  },
  {
    name: 'files',
    path: '/files',
    component: 'files',
    title: '文件管理',
    permission: 'file:list',
    icon: 'ant-design:folder-open-outlined'
  },
  {
    name: 'logs',
    path: '/logs',
    component: 'logs',
    title: '审计日志',
    permission: 'log:operation:list',
    icon: 'ant-design:file-search-outlined'
  }
];

const directoryDefinitions = {
  system: { name: 'system', path: '/system', title: '系统管理', icon: 'ant-design:setting-outlined', order: 1 },
  monitor: { name: 'monitor', path: '/monitor', title: '运行监控', icon: 'ant-design:dashboard-outlined', order: 2 }
} as const;

function backendComponentOf(definition: RouteDefinition) {
  const componentMap: Record<string, string> = {
    'monitor-online-users': 'monitor/online-users',
    files: 'files/index',
    logs: 'system/logs'
  };

  return componentMap[definition.component] || definition.component.replaceAll('-', '/');
}

function toRoute(definition: RouteDefinition): ElegantConstRoute {
  return {
    name: definition.name,
    path: definition.path,
    component: definition.parent ? `view.${definition.name}` : `layout.base$view.${definition.name}`,
    meta: {
      title: definition.title,
      icon: definition.icon,
      order: routeDefinitions.findIndex(item => item.name === definition.name),
      permission: definition.permission
    }
  } as ElegantConstRoute;
}

function buildRoutes(routes: AppRoute[]) {
  const availableComponents = new Set(routes.filter(route => route.menuType === 'MENU').map(route => route.component));
  const result: ElegantConstRoute[] = [
    {
      name: 'home',
      path: '/home',
      component: 'layout.base$view.home',
      meta: { title: '工作台', icon: 'ant-design:home-outlined', order: 0 }
    } as ElegantConstRoute,
    {
      name: 'user-center',
      path: '/user-center',
      component: 'layout.base$view.user-center',
      meta: { title: '个人中心', hideInMenu: true }
    } as ElegantConstRoute
  ];

  (Object.keys(directoryDefinitions) as Array<keyof typeof directoryDefinitions>).forEach(key => {
    const children = routeDefinitions
      .filter(definition => definition.parent === key && availableComponents.has(backendComponentOf(definition)))
      .map(toRoute);
    if (children.length) {
      const directory = directoryDefinitions[key];
      result.push({
        name: directory.name,
        path: directory.path,
        component: 'layout.base',
        meta: { title: directory.title, icon: directory.icon, order: directory.order },
        children
      } as ElegantConstRoute);
    }
  });

  routeDefinitions
    .filter(definition => !definition.parent)
    .forEach(definition => {
      if (availableComponents.has(backendComponentOf(definition))) result.push(toRoute(definition));
    });

  return result;
}

export function fetchGetConstantRoutes() {
  return Promise.resolve({ data: [] as ElegantConstRoute[], error: null });
}

export function fetchGetBackendRoutes() {
  return request<AppRoute[]>({ url: '/auth/routes' });
}

export async function fetchGetUserRoutes() {
  const response = await fetchGetBackendRoutes();
  return response.error
    ? response
    : { ...response, data: { routes: buildRoutes(response.data), home: 'home' as LastLevelRouteKey } };
}

export function fetchIsRouteExist(routeName: string) {
  return Promise.resolve({
    data: routeName === 'home' || routeDefinitions.some(route => route.name === routeName),
    error: null
  });
}
