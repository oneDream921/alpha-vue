import { reactive, readonly } from 'vue'

export interface UserProfile {
    id: string
    username: string
    nickname?: string
    roles: string[]
    permissions: string[]
}

export interface AppRoute {
    path: string
    name: string
    title: string
    icon?: string
    children?: AppRoute[]
}

interface AuthState {
    token: string | null
    profile: UserProfile | null
    routes: AppRoute[]
}

const storageKey = 'alpha-auth'

function readState(): AuthState {
    if (typeof window === 'undefined') {
        return { token: null, profile: null, routes: [] }
    }

    try {
        const stored = window.localStorage.getItem(storageKey)
        return stored
            ? (JSON.parse(stored) as AuthState)
            : { token: null, profile: null, routes: [] }
    } catch {
        return { token: null, profile: null, routes: [] }
    }
}

const state = reactive<AuthState>(readState())

function persist() {
    if (typeof window !== 'undefined') {
        window.localStorage.setItem(storageKey, JSON.stringify(state))
    }
}

export const authStore = {
    state: readonly(state),
    getToken: () => state.token,
    setToken(token: string) {
        state.token = token
        persist()
    },
    setProfile(profile: UserProfile) {
        state.profile = profile
        persist()
    },
    setRoutes(routes: AppRoute[]) {
        state.routes = routes
        persist()
    },
    clearAuth() {
        state.token = null
        state.profile = null
        state.routes = []
        if (typeof window !== 'undefined') {
            window.localStorage.removeItem(storageKey)
        }
    },
}

export type AuthStore = Pick<typeof authStore, 'getToken' | 'clearAuth'>
