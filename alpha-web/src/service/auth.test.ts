import AxiosMockAdapter from 'axios-mock-adapter'
import { describe, expect, it } from 'vitest'

import { authApi } from './auth'
import { http } from './http'

describe('authApi', () => {
    it('always sends the registered admin client id', async () => {
        const mock = new AxiosMockAdapter(http)
        let requestBody: { clientId?: string } | undefined
        mock.onPost('/auth/login').reply((config) => {
            requestBody = JSON.parse(config.data as string) as {
                clientId?: string
            }
            return [
                200,
                {
                    code: 0,
                    data: {
                        token: 'token',
                        tokenType: 'Bearer',
                        expiresIn: 3600,
                    },
                    traceId: 'trace-id',
                },
            ]
        })

        const response = await authApi.login({
            username: 'admin',
            password: 'password',
        })

        expect(response.data.data.token).toBe('token')
        expect(requestBody?.clientId).toBe('pc-admin')
        mock.reset()
    })
})
