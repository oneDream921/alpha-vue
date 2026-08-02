import { describe, expect, it, vi } from 'vitest'

const { get, put } = vi.hoisted(() => ({ get: vi.fn(), put: vi.fn() }))

vi.mock('./http', () => ({
    http: { get, put },
}))

import { logApi } from './logs'

describe('logApi', () => {
    it('queries operation logs through the protected endpoint', () => {
        logApi.operations(2, 20, { status: 0, handlingStatus: 1 })

        expect(get).toHaveBeenCalledWith('/logs/operations', {
            params: { page: 2, size: 20, status: 0, handlingStatus: 1 },
        })
    })

    it('loads operation details through the separately protected endpoint', () => {
        logApi.operationDetail(42)

        expect(get).toHaveBeenCalledWith('/logs/operations/42')
    })
})
