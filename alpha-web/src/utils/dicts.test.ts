import { describe, expect, it } from 'vitest'

import {
    defaultDictValue,
    dictLabel,
    dictLabels,
    dictValueTokens,
} from './dicts'

const options = [
    { label: '草稿', value: 'draft', sortOrder: 1, isDefault: 1 },
    { label: '启用', value: 'enabled', sortOrder: 2, isDefault: 0 },
]

describe('dictionary helpers', () => {
    it('normalizes single, array, and separated values', () => {
        expect(dictValueTokens('draft, enabled')).toEqual(['draft', 'enabled'])
        expect(dictValueTokens(['draft', 1, true])).toEqual([
            'draft',
            '1',
            'true',
        ])
        expect(dictValueTokens(null)).toEqual([])
    })

    it('maps values to labels while preserving unmatched values when requested', () => {
        expect(dictLabels(options, 'draft,missing')).toEqual([
            '草稿',
            'missing',
        ])
        expect(dictLabel(options, 'draft,enabled')).toBe('草稿,启用')
        expect(dictLabel(options, 'missing', { showValue: false })).toBe('')
    })

    it('finds the first default dictionary value', () => {
        expect(defaultDictValue(options)).toBe('draft')
    })
})
