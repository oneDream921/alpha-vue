import { request } from '../request';

export interface PageResponse<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface BaseEntity {
  id: number;
  status: number;
  createdAt?: string;
}

export interface User extends BaseEntity {
  username: string;
  nickname: string;
  avatar?: string;
  email?: string;
  phone?: string;
  deptId?: number;
  mustChangePassword: number;
  roleIds: number[];
}

export interface Role extends BaseEntity {
  name: string;
  code: string;
  sortOrder: number;
  remark?: string;
}

export interface Menu extends BaseEntity {
  parentId?: number;
  title: string;
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON';
  path?: string;
  component?: string;
  permission?: string;
  icon?: string;
  sortOrder: number;
  visible: number;
}

export interface Dept extends BaseEntity {
  parentId?: number;
  name: string;
  sortOrder: number;
}

export interface DictType extends BaseEntity {
  typeCode: string;
  typeName: string;
  remark?: string;
  updatedAt?: string;
}

export interface DictItem extends BaseEntity {
  typeId: number;
  label: string;
  value: string;
  sortOrder: number;
  isDefault: number;
  remark?: string;
  updatedAt?: string;
}

export interface EnabledDictItem {
  label: string;
  value: string;
  sortOrder: number;
  isDefault: number;
}

export interface DictCacheRefreshResult {
  typeCount: number;
}

export interface UserCreate {
  username: string;
  password: string;
  nickname: string;
  avatar?: string;
  email?: string;
  phone?: string;
  deptId?: number;
}

export interface UserUpdate {
  nickname: string;
  avatar?: string;
  email?: string;
  phone?: string;
  deptId?: number;
  status: number;
}

export interface RoleCreate {
  name: string;
  code: string;
  sortOrder: number;
  status: number;
  remark?: string;
}

export type RoleUpdate = Omit<RoleCreate, 'code'>;
export type MenuSave = Omit<Menu, 'id' | 'createdAt'>;
export type DeptSave = Omit<Dept, 'id' | 'createdAt'>;

export interface DictTypeSave {
  typeCode: string;
  typeName: string;
  status: number;
  remark?: string;
}

export interface DictItemSave {
  label: string;
  value: string;
  sortOrder: number;
  status: number;
  isDefault: number;
  remark?: string;
}

type EntityName = 'users' | 'roles' | 'menus' | 'depts';

function resource<T, Create, Update>(name: EntityName) {
  return {
    page: (page = 1, size = 10) =>
      request<PageResponse<T>>({ url: `/system/${name}`, method: 'get', params: { page, size } }),
    create: (data: Create) => request<T>({ url: `/system/${name}`, method: 'post', data }),
    update: (id: number, data: Update) => request<T>({ url: `/system/${name}/${id}`, method: 'put', data }),
    delete: (id: number) => request<null>({ url: `/system/${name}/${id}`, method: 'delete' })
  };
}

export const userApi = {
  ...resource<User, UserCreate, UserUpdate>('users'),
  page: (params: { page?: number; size?: number; deptId?: number; keyword?: string } = {}) =>
    request<PageResponse<User>>({
      url: '/system/users',
      method: 'get',
      params: { page: params.page ?? 1, size: params.size ?? 10, deptId: params.deptId, keyword: params.keyword }
    }),
  assignRoles: (id: number, roleIds: number[]) =>
    request<null>({ url: `/system/users/${id}/roles`, method: 'put', data: { roleIds } }),
  kickout: (id: number) => request<null>({ url: `/system/users/${id}/kickout`, method: 'put' }),
  resetPassword: (id: number, newPassword: string) =>
    request<null>({ url: `/system/users/${id}/password`, method: 'put', data: { newPassword } })
};

export const roleApi = {
  ...resource<Role, RoleCreate, RoleUpdate>('roles'),
  assignMenus: (id: number, menuIds: number[]) =>
    request<null>({ url: `/system/roles/${id}/menus`, method: 'put', data: { menuIds } }),
  menuIds: (id: number) => request<number[]>({ url: `/system/roles/${id}/menus`, method: 'get' })
};

export const menuApi = {
  ...resource<Menu, MenuSave, MenuSave>('menus'),
  assignable: () => request<Menu[]>({ url: '/system/menus/assignable', method: 'get' })
};

export const deptApi = resource<Dept, DeptSave, DeptSave>('depts');

export const dictApi = {
  pageTypes: (page = 1, size = 10) =>
    request<PageResponse<DictType>>({ url: '/system/dict-types', method: 'get', params: { page, size } }),
  getType: (id: number) => request<DictType>({ url: `/system/dict-types/${id}`, method: 'get' }),
  createType: (data: DictTypeSave) => request<DictType>({ url: '/system/dict-types', method: 'post', data }),
  updateType: (id: number, data: DictTypeSave) =>
    request<DictType>({ url: `/system/dict-types/${id}`, method: 'put', data }),
  deleteType: (id: number) => request<null>({ url: `/system/dict-types/${id}`, method: 'delete' }),
  pageItems: (typeId: number, page = 1, size = 10) =>
    request<PageResponse<DictItem>>({
      url: `/system/dict-types/${typeId}/items`,
      method: 'get',
      params: { page, size }
    }),
  createItem: (typeId: number, data: DictItemSave) =>
    request<DictItem>({ url: `/system/dict-types/${typeId}/items`, method: 'post', data }),
  updateItem: (id: number, data: DictItemSave) =>
    request<DictItem>({ url: `/system/dict-items/${id}`, method: 'put', data }),
  deleteItem: (id: number) => request<null>({ url: `/system/dict-items/${id}`, method: 'delete' }),
  refreshCache: () => request<DictCacheRefreshResult>({ url: '/system/dicts/cache', method: 'put' }),
  enabledItems: (typeCode: string) =>
    request<EnabledDictItem[]>({ url: `/system/dicts/${typeCode}/items`, method: 'get' })
};
