import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

const dictionaryPage = readFileSync('src/views/system/dicts.vue', 'utf8')

describe('dictionary item table', () => {
    it('uses a reference-style management table and remains horizontally scrollable', () => {
        expect(dictionaryPage).not.toContain("from 'vxe-table'")
        expect(dictionaryPage).toContain('<a-table')
        expect(dictionaryPage).not.toContain('TableColumnSetting')
        expect(dictionaryPage).toContain("title: '字典标签'")
        expect(dictionaryPage).toContain("title: '备注'")
        expect(dictionaryPage).toContain("title: '创建时间'")
        expect(dictionaryPage).toContain('formatTime(record.createdAt)')
        expect(dictionaryPage).toContain("padStart(2, '0')")
    })

    it('provides an explicit dictionary cache refresh action', () => {
        expect(dictionaryPage).toContain('刷新缓存')
        expect(dictionaryPage).toContain(
            'v-permission="\'system:dict:update\'"',
        )
        expect(dictionaryPage).toContain('dictApi.refreshCache()')
    })

    it('shows the complete dictionary type when a truncated row is hovered', () => {
        expect(dictionaryPage).toContain('<a-tooltip')
        expect(dictionaryPage).toContain(
            '`${type.typeName}（${type.typeCode}）`',
        )
    })
})
