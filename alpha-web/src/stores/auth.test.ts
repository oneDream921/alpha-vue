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

    it('uses session storage unless remember-me is enabled', () => {
        authStore.setToken('session-token')
        expect(sessionStorage.getItem('alpha-auth')).toContain('session-token')
        expect(localStorage.getItem('alpha-auth')).toBeNull()
        authStore.setToken('remembered-token', true)
        expect(localStorage.getItem('alpha-auth')).toContain('remembered-token')
        expect(sessionStorage.getItem('alpha-auth')).toBeNull()
    })
})
