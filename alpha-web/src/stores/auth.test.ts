import { beforeEach, describe, expect, it } from 'vitest'

import { authStore } from './auth'

describe('authStore', () => {
    beforeEach(() => authStore.clearAuth())

    it('stores a complete authenticated session', () => {
        authStore.setSession(
            'token-value',
            {
                id: 1,
                username: 'admin',
                nickname: 'Administrator',
                roles: ['SUPER_ADMIN'],
                permissions: ['system:user:list'],
                mustChangePassword: false,
            },
            [],
        )

        expect(authStore.getToken()).toBe('token-value')
        expect(authStore.hasPermission('system:user:list')).toBe(true)
        expect(authStore.hasPermission('system:user:delete')).toBe(false)
    })

    it('clears all session data after a rejected login', () => {
        authStore.setToken('stale-token')
        authStore.clearAuth()

        expect(authStore.getToken()).toBeNull()
        expect(authStore.state.profile).toBeNull()
        expect(authStore.state.routes).toEqual([])
    })
})
