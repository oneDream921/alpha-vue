import { beforeEach, describe, expect, it, vi } from 'vitest'

const { enabledItems } = vi.hoisted(() => ({
    enabledItems: vi.fn(),
}))

vi.mock('@/service/system/index', () => ({
    dictApi: { enabledItems },
}))

import { dictStore } from './dict'

describe('dictStore', () => {
    beforeEach(() => {
        dictStore.clear()
        enabledItems.mockReset()
    })

    it('loads a dictionary once and reuses the cached value', async () => {
        enabledItems.mockResolvedValue({
            data: {
                data: [
                    {
                        label: '启用',
                        value: 'enabled',
                        sortOrder: 1,
                        isDefault: 1,
                    },
                ],
            },
        })

        const first = await dictStore.load(' dict-test.status ')
        const second = await dictStore.load('dict-test.status')

        expect(first).toEqual(second)
        expect(enabledItems).toHaveBeenCalledOnce()
        expect(enabledItems).toHaveBeenCalledWith('dict-test.status')
    })

    it('shares a pending request for the same type', async () => {
        enabledItems.mockResolvedValue({
            data: { data: [] },
        })

        await Promise.all([
            dictStore.load('dict-test.status'),
            dictStore.load('dict-test.status'),
        ])

        expect(enabledItems).toHaveBeenCalledOnce()
    })
})
