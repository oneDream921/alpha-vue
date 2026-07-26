import { describe, expect, it } from 'vitest'

import { configPageFromTableChange } from './configs.pagination'

describe('configPageFromTableChange', () => {
    it('uses the pagination object emitted by the table change event', () => {
        expect(
            configPageFromTableChange({ current: 3, pageSize: 20 }, 1, 10),
        ).toEqual({ page: 3, pageSize: 20 })
    })

    it('keeps current values when optional pagination fields are absent', () => {
        expect(configPageFromTableChange({}, 2, 10)).toEqual({
            page: 2,
            pageSize: 10,
        })
    })
})
