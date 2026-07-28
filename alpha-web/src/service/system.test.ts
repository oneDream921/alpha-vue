import { describe, expect, it, vi } from 'vitest'

const { put } = vi.hoisted(() => ({ put: vi.fn() }))

vi.mock('./http', () => ({
    http: { put },
}))

import { dictApi } from './system'

describe('dictApi', () => {
    it('refreshes dictionary cache through the system endpoint', () => {
        dictApi.refreshCache()

        expect(put).toHaveBeenCalledWith('/system/dicts/cache')
    })
})
