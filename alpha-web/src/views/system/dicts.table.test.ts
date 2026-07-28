import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

const dictionaryPage = readFileSync('src/views/system/dicts.vue', 'utf8')

describe('dictionary item table', () => {
    it('shows every management field and remains horizontally scrollable', () => {
        expect(dictionaryPage).toContain(':scroll="{ x: 820 }"')
        expect(dictionaryPage).toContain('title="状态"')
        expect(dictionaryPage).toContain('title="默认"')
    })

    it('provides an explicit dictionary cache refresh action', () => {
        expect(dictionaryPage).toContain('刷新缓存')
        expect(dictionaryPage).toContain(
            'v-permission="\'system:dict:update\'"',
        )
        expect(dictionaryPage).toContain('dictApi.refreshCache()')
    })
})
