import AxiosMockAdapter from 'axios-mock-adapter'
import { describe, expect, it, vi } from 'vitest'

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
})
