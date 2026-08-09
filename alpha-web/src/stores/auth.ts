import { defineStore } from 'pinia'

import { pinia } from './pinia'

export interface UserProfile {
    id: number
    username: string
    nickname?: string
    avatar?: string
    email?: string
    phone?: string
    deptId?: number
    roles: string[]
    permissions: string[]
    mustChangePassword: boolean
}

export interface AppRoute {
    id: number
    parentId: number
    title: string
    menuType: 'DIRECTORY' | 'MENU' | 'BUTTON'
    path?: string
    component?: string
    permission?: string
    icon?: string
    sortOrder: number
}

interface AuthState {
    token: string | null
    profile: UserProfile | null
    routes: AppRoute[]
}

const storageKey = 'alpha-auth'

function readState(): AuthState {
    const storages = [localStorageOrNull(), sessionStorageOrNull()].filter(
        (storage): storage is Storage => storage !== null,
    )
    if (!storages.length) {
        return { token: null, profile: null, routes: [] }
    }

    for (const storage of storages) {
        try {
            const stored = storage.getItem(storageKey)
            if (stored) return JSON.parse(stored) as AuthState
        } catch {
            // Try the next storage location.
        }
    }
    return { token: null, profile: null, routes: [] }
}

const useAuthStore = defineStore('auth', {
    state: (): AuthState => readState(),
})
const store = useAuthStore(pinia)

let persistenceStorage: 'local' | 'session' = 'session'

function persist() {
    const storage =
        persistenceStorage === 'local'
            ? localStorageOrNull()
            : sessionStorageOrNull()
    if (storage) {
        storage.setItem(storageKey, JSON.stringify(store.$state))
    }
}

function localStorageOrNull(): Storage | null {
    try {
        return typeof window === 'undefined' ? null : window.localStorage
    } catch {
        return null
    }
}

function sessionStorageOrNull(): Storage | null {
    try {
        return typeof window === 'undefined' ? null : window.sessionStorage
    } catch {
        return null
    }
}

function setPersistence(rememberMe: boolean) {
    persistenceStorage = rememberMe ? 'local' : 'session'
    const otherStorage = rememberMe
        ? sessionStorageOrNull()
        : localStorageOrNull()
    otherStorage?.removeItem(storageKey)
}

export const authStore = {
    get state() {
        return store.$state
    },
    getToken: () => store.token,
    setToken(token: string, rememberMe = false) {
        setPersistence(rememberMe)
        store.token = token
        persist()
    },
    setProfile(profile: UserProfile) {
        store.profile = profile
        persist()
    },
    setRoutes(routes: AppRoute[]) {
        store.routes = routes
        persist()
    },
    setSession(
        token: string,
        profile: UserProfile,
        routes: AppRoute[],
        rememberMe = false,
    ) {
        setPersistence(rememberMe)
        store.$patch({ token, profile, routes })
        persist()
    },
    hasPermission(permission: string) {
        return (
            store.profile?.permissions.some(
                (item) => item === '*' || item === permission,
            ) ?? false
        )
    },
    clearAuth() {
        localStorageOrNull()?.removeItem(storageKey)
        sessionStorageOrNull()?.removeItem(storageKey)
        store.$reset()
    },
}

export type AuthStore = Pick<typeof authStore, 'getToken' | 'clearAuth'>
