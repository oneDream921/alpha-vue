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
    const storage = localStorageOrNull()
    if (!storage) {
        return { token: null, profile: null, routes: [] }
    }

    try {
        const stored = storage.getItem(storageKey)
        return stored
            ? (JSON.parse(stored) as AuthState)
            : { token: null, profile: null, routes: [] }
    } catch {
        return { token: null, profile: null, routes: [] }
    }
}

const useAuthStore = defineStore('auth', {
    state: (): AuthState => readState(),
})
const store = useAuthStore(pinia)

function persist() {
    const storage = localStorageOrNull()
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

export const authStore = {
    get state() {
        return store.$state
    },
    getToken: () => store.token,
    setToken(token: string) {
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
    setSession(token: string, profile: UserProfile, routes: AppRoute[]) {
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
        store.$reset()
    },
}

export type AuthStore = Pick<typeof authStore, 'getToken' | 'clearAuth'>
