import AxiosMockAdapter from 'axios-mock-adapter'
import { describe, expect, it, vi } from 'vitest'

const { error, warning, router } = vi.hoisted(() => ({
    error: vi.fn(),
    warning: vi.fn((options: { onOk?: () => unknown }) => {
        void options.onOk?.()
    }),
    router: {
        currentRoute: { value: { name: 'dashboard', fullPath: '/dashboard' } },
        replace: vi.fn(),
    },
}))

vi.mock('ant-design-vue', () => ({
    message: { error },
    Modal: { warning },
}))

vi.mock('@/router', () => ({ default: router }))

import { createHttpClient } from './http'

describe('createHttpClient', () => {
    it('attaches the current Bearer token to requests', async () => {
        const auth = {
            getToken: () => 'session-token',
            clearAuth: vi.fn(),
        }
        const client = createHttpClient(auth)
        const mock = new AxiosMockAdapter(client)

        mock.onGet('/profile').reply((config) => [
            200,
            { code: 0, data: config.headers?.Authorization },
        ])

        const response = await client.get<{ code: number; data: string }>(
            '/profile',
        )

        expect(response.data.data).toBe('Bearer session-token')
    })

    it('clears the auth store when an API response is unauthorized', async () => {
        const auth = {
            getToken: () => 'session-token',
            clearAuth: vi.fn(),
        }
        const client = createHttpClient(auth)
        const mock = new AxiosMockAdapter(client)

        mock.onGet('/profile').reply(401, {
            code: 401,
            message: 'Unauthorized',
        })

        await expect(client.get('/profile')).rejects.toMatchObject({
            response: { status: 401 },
        })
        expect(auth.clearAuth).toHaveBeenCalledOnce()
    })

    it('does not show a generic error for rejected login credentials', async () => {
        error.mockClear()
        const auth = {
            getToken: () => null,
            clearAuth: vi.fn(),
        }
        const client = createHttpClient(auth)
        const mock = new AxiosMockAdapter(client)

        mock.onPost('/auth/login').reply(401, {
            code: 401,
            message: '账号或密码错误',
        })

        await expect(client.post('/auth/login')).rejects.toMatchObject({
            response: { status: 401 },
        })
        expect(error).not.toHaveBeenCalled()
    })

    it('recognizes login requests with query parameters by method and path', async () => {
        error.mockClear()
        const auth = {
            getToken: () => null,
            clearAuth: vi.fn(),
        }
        const client = createHttpClient(auth)
        const mock = new AxiosMockAdapter(client)

        mock.onPost('/auth/login?source=login-page').reply(401, {
            code: 401,
            message: '账号或密码错误',
        })

        await expect(
            client.post('/auth/login?source=login-page'),
        ).rejects.toMatchObject({ response: { status: 401 } })
        expect(error).not.toHaveBeenCalled()
    })

    it('opens only one login prompt for concurrent unauthorized responses', async () => {
        warning.mockClear()
        const auth = {
            getToken: () => 'session-token',
            clearAuth: vi.fn(),
        }
        const client = createHttpClient(auth)
        const mock = new AxiosMockAdapter(client)

        mock.onGet('/profile').reply(401, {
            code: 401,
            message: 'Unauthorized',
        })

        await Promise.allSettled([
            client.get('/profile'),
            client.get('/profile'),
            client.get('/profile'),
        ])
        await new Promise((resolve) => setTimeout(resolve, 0))

        expect(warning).toHaveBeenCalledOnce()
    })
})
