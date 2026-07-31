import Antd from 'ant-design-vue'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const { login, profile, routes, captcha, replace } = vi.hoisted(() => ({
    login: vi.fn(),
    profile: vi.fn(),
    routes: vi.fn(),
    captcha: vi.fn(),
    replace: vi.fn(),
}))

vi.mock('@/service/auth', () => ({
    authApi: { login, profile, routes, captcha },
}))
vi.mock('vue-router', () => ({
    useRoute: () => ({ query: {} }),
    useRouter: () => ({ replace }),
}))
vi.mock('@/router', () => ({
    clearManagementRoutes: vi.fn(),
    ensureManagementRoutes: vi.fn(),
}))

import Login from './index.vue'
import { authStore } from '@/stores/auth'

describe('login page', () => {
    it('stores the token before loading profile and routes', async () => {
        captcha.mockResolvedValue({
            data: { data: { enabled: false, captchaId: null, image: null } },
        })
        login.mockResolvedValue({
            data: {
                data: { token: 'token', tokenType: 'Bearer', expiresIn: 3600 },
            },
        })
        profile.mockImplementation(async () => {
            expect(authStore.getToken()).toBe('token')
            return {
                data: {
                    data: {
                        id: 1,
                        username: 'admin',
                        roles: [],
                        permissions: [],
                        mustChangePassword: false,
                    },
                },
            }
        })
        routes.mockResolvedValue({ data: { data: [] } })

        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('password')
        await wrapper.get('form').trigger('submit')
        await flushPromises()

        expect(authStore.getToken()).toBe('token')
        expect(authStore.state.profile?.username).toBe('admin')
        expect(replace).toHaveBeenCalledWith('/')
        authStore.clearAuth()
    })

    it('clears session data when login fails', async () => {
        authStore.setSession(
            'stale-token',
            {
                id: 1,
                username: 'admin',
                roles: [],
                permissions: [],
                mustChangePassword: false,
            },
            [],
        )
        captcha.mockResolvedValue({
            data: { data: { enabled: false, captchaId: null, image: null } },
        })
        login.mockRejectedValue(new Error('invalid credentials'))

        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('password')
        await wrapper.get('form').trigger('submit')
        await flushPromises()

        expect(authStore.getToken()).toBeNull()
        expect(authStore.state.profile).toBeNull()
        expect(authStore.state.routes).toEqual([])
    })
})
