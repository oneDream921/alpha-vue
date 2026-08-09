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
                suppressGlobalErrorMessage: false,
            },
        )
    })

    it('can silence expected private preview failures', () => {
        fileApi.content('/api/files/9/content', 'arraybuffer', { silent: true })

        expect(get).toHaveBeenLastCalledWith('/api/files/9/content', {
            baseURL: '',
            responseType: 'arraybuffer',
            suppressGlobalErrorMessage: true,
        })
    })

    it('requests a fresh access URL for a persisted file', () => {
        fileApi.accessUrl(9)

        expect(get).toHaveBeenLastCalledWith('/files/9/access-url')
    })
})
