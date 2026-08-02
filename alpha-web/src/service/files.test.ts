import { describe, expect, it, vi } from 'vitest'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))

vi.mock('./http', () => ({
    http: { get },
}))

import { fileApi } from './files'

describe('fileApi', () => {
    it('reads private file content without duplicating the /api base path', () => {
        fileApi.content('/api/files/9/content?expires=123&signature=test')

        expect(get).toHaveBeenCalledWith(
            '/api/files/9/content?expires=123&signature=test',
            {
                baseURL: '',
                responseType: 'arraybuffer',
            },
        )
    })
})
