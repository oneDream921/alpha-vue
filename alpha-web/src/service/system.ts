import { http, type ApiResponse } from './http'

export interface PageResponse<T> {
    records: T[]
    total: number
    page: number
    size: number
}

export interface BaseEntity {
    id: number
    status: number
    createdAt?: string
}

export interface User extends BaseEntity {
    username: string
    nickname: string
    avatar?: string
    email?: string
    phone?: string
    deptId?: number
    mustChangePassword: number
    roleIds: number[]
}

export interface Role extends BaseEntity {
    name: string
    code: string
    sortOrder: number
    remark?: string
}

export interface Menu extends BaseEntity {
    parentId?: number
    title: string
    menuType: 'DIRECTORY' | 'MENU' | 'BUTTON'
    path?: string
    component?: string
    permission?: string
    icon?: string
    sortOrder: number
    visible: number
}

export interface Dept extends BaseEntity {
    parentId?: number
    name: string
    sortOrder: number
}

export interface Config {
    id: number
    configName: string
    configKey: string
    configValue?: string
    configGroup: string
    dataType: ConfigDataType
    enabled: boolean
    description?: string
    domain: string
    sensitive: boolean
    dynamic: boolean
    enumValues: string[]
    createdAt?: string
    updatedAt?: string
}

export type ConfigDataType = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'ENUM'

export interface ConfigDefinition {
    id: number
    configKey: string
    configName: string
    configGroup: string
    valueType: ConfigDataType
    defaultValue?: string
    integerMin?: number
    integerMax?: number
    stringMaxLength?: number
    stringPattern?: string
    sensitive: boolean
    dynamic: boolean
    runtimeBinding?: string
    status: 'DRAFT' | 'PUBLISHED' | 'DISABLED'
    enumValues: string[]
}

export interface ConfigDefinitionSave {
    configKey: string
    configName: string
    valueType: ConfigDataType
    defaultValue: string
    integerMin?: number
    integerMax?: number
    stringMaxLength?: number
    stringPattern?: string
    enumValues?: string
    sensitive: boolean
    dynamic: boolean
    runtimeBinding?: string
    status: 'DRAFT' | 'PUBLISHED' | 'DISABLED'
}

export interface DictType extends BaseEntity {
    typeCode: string
    typeName: string
    remark?: string
    updatedAt?: string
}

export interface DictItem extends BaseEntity {
    typeId: number
    label: string
    value: string
    sortOrder: number
    isDefault: number
    remark?: string
    updatedAt?: string
}

export interface EnabledDictItem {
    label: string
    value: string
    sortOrder: number
    isDefault: number
}

export interface DictCacheRefreshResult {
    typeCount: number
}

type EntityName = 'users' | 'roles' | 'menus' | 'depts' | 'configs'

function resource<T, Create, Update>(name: EntityName) {
    return {
        page: (page = 1, size = 10) =>
            http.get<ApiResponse<PageResponse<T>>>(`/system/${name}`, {
                params: { page, size },
            }),
        create: (payload: Create) =>
            http.post<ApiResponse<T>>(`/system/${name}`, payload),
        update: (id: number, payload: Update) =>
            http.put<ApiResponse<T>>(`/system/${name}/${id}`, payload),
        delete: (id: number) =>
            http.delete<ApiResponse<null>>(`/system/${name}/${id}`),
    }
}

export interface UserCreate {
    username: string
    password: string
    nickname: string
    avatar?: string
    email?: string
    phone?: string
    deptId?: number
}
export interface UserUpdate {
    nickname: string
    avatar?: string
    email?: string
    phone?: string
    deptId?: number
    status: number
}
export interface RoleCreate {
    name: string
    code: string
    sortOrder: number
    status: number
    remark?: string
}
export type RoleUpdate = Omit<RoleCreate, 'code'>
export type MenuSave = Omit<Menu, 'id' | 'createdAt'>
export type DeptSave = Omit<Dept, 'id' | 'createdAt'>
export interface ConfigSave {
    configKey: string
    configValue: string
    enabled: boolean
}
export interface DictTypeSave {
    typeCode: string
    typeName: string
    status: number
    remark?: string
}
export interface DictItemSave {
    label: string
    value: string
    sortOrder: number
    status: number
    isDefault: number
    remark?: string
}

export const userApi = {
    ...resource<User, UserCreate, UserUpdate>('users'),
    page: (page = 1, size = 10, deptId?: number) =>
        http.get<ApiResponse<PageResponse<User>>>('/system/users', {
            params: { page, size, deptId },
        }),
    assignRoles: (id: number, roleIds: number[]) =>
        http.put<ApiResponse<null>>(`/system/users/${id}/roles`, { roleIds }),
    kickout: (id: number) =>
        http.put<ApiResponse<null>>(`/system/users/${id}/kickout`),
    resetPassword: (id: number, newPassword: string) =>
        http.put<ApiResponse<null>>(`/system/users/${id}/password`, {
            newPassword,
        }),
}
export const roleApi = {
    ...resource<Role, RoleCreate, RoleUpdate>('roles'),
    assignMenus: (id: number, menuIds: number[]) =>
        http.put<ApiResponse<null>>(`/system/roles/${id}/menus`, { menuIds }),
    menuIds: (id: number) =>
        http.get<ApiResponse<number[]>>(`/system/roles/${id}/menus`),
}
export const menuApi = {
    ...resource<Menu, MenuSave, MenuSave>('menus'),
    assignable: () => http.get<ApiResponse<Menu[]>>('/system/menus/assignable'),
}
export const deptApi = resource<Dept, DeptSave, DeptSave>('depts')
export const configApi = {
    ...resource<Config, ConfigSave, ConfigSave>('configs'),
    definitions: (page = 1, size = 100) =>
        http.get<ApiResponse<PageResponse<ConfigDefinition>>>(
            '/system/configs/definitions',
            { params: { page, size } },
        ),
    createDefinition: (payload: ConfigDefinitionSave) =>
        http.post<ApiResponse<ConfigDefinition>>(
            '/system/configs/definitions',
            payload,
        ),
    updateDefinition: (id: number, payload: ConfigDefinitionSave) =>
        http.put<ApiResponse<ConfigDefinition>>(
            `/system/configs/definitions/${id}`,
            payload,
        ),
}
export const dictApi = {
    pageTypes: (page = 1, size = 10) =>
        http.get<ApiResponse<PageResponse<DictType>>>('/system/dict-types', {
            params: { page, size },
        }),
    getType: (id: number) =>
        http.get<ApiResponse<DictType>>(`/system/dict-types/${id}`),
    createType: (payload: DictTypeSave) =>
        http.post<ApiResponse<DictType>>('/system/dict-types', payload),
    updateType: (id: number, payload: DictTypeSave) =>
        http.put<ApiResponse<DictType>>(`/system/dict-types/${id}`, payload),
    deleteType: (id: number) =>
        http.delete<ApiResponse<null>>(`/system/dict-types/${id}`),
    pageItems: (typeId: number, page = 1, size = 10) =>
        http.get<ApiResponse<PageResponse<DictItem>>>(
            `/system/dict-types/${typeId}/items`,
            {
                params: { page, size },
            },
        ),
    createItem: (typeId: number, payload: DictItemSave) =>
        http.post<ApiResponse<DictItem>>(
            `/system/dict-types/${typeId}/items`,
            payload,
        ),
    updateItem: (id: number, payload: DictItemSave) =>
        http.put<ApiResponse<DictItem>>(`/system/dict-items/${id}`, payload),
    deleteItem: (id: number) =>
        http.delete<ApiResponse<null>>(`/system/dict-items/${id}`),
    refreshCache: () =>
        http.put<ApiResponse<DictCacheRefreshResult>>('/system/dicts/cache'),
    enabledItems: (typeCode: string) =>
        http.get<ApiResponse<EnabledDictItem[]>>(
            `/system/dicts/${typeCode}/items`,
        ),
}
