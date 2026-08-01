import { describe, expect, it, vi } from 'vitest'

const { get, remove } = vi.hoisted(() => ({ get: vi.fn(), remove: vi.fn() }))

vi.mock('./http', () => ({
    http: { get, delete: remove },
}))

import { redisApi } from './redis'

describe('redisApi', () => {
    it('requests Redis metrics without query parameters', () => {
        redisApi.metrics()

        expect(get).toHaveBeenCalledWith('/monitor/redis/metrics')
    })

    it('uses the bounded cursor endpoint for a managed prefix', () => {
        redisApi.keys({
            prefix: 'auth:',
            cursor: '0',
            count: 50,
            keyword: 'login',
        })

        expect(get).toHaveBeenCalledWith('/monitor/redis/keys', {
            params: {
                prefix: 'auth:',
                cursor: '0',
                count: 50,
                keyword: 'login',
            },
        })
    })
})
