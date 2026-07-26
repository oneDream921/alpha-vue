import { describe, expect, it } from 'vitest'

import { validateItemValue, validateTypeCode } from './dicts.validation'

describe('dictionary validation helpers', () => {
    it('rejects invalid type codes', () => {
        expect(validateTypeCode('')).toBe('请输入类型编码')
        expect(validateTypeCode('1status')).toBe(
            '类型编码需以字母开头，仅含字母、数字、点、下划线或连字符',
        )
    })

    it('accepts string item values and enforces their length', () => {
        expect(validateItemValue('enabled')).toBeUndefined()
        expect(validateItemValue('x'.repeat(129))).toBe(
            '字典项值不能超过 128 个字符',
        )
    })
})
