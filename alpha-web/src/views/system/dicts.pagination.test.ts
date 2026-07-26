import { describe, expect, it } from 'vitest'

import {
    dictionaryTypeRow,
    dictPageFromTableChange,
    itemPageForTypeSelection,
} from './dicts.pagination'

describe('dictionary pagination helpers', () => {
    it('keeps table pagination values when the event omits them', () => {
        expect(dictPageFromTableChange({}, 2, 20)).toEqual({
            page: 2,
            pageSize: 20,
        })
    })

    it('resets item pagination and avoids loading when no type is selected', () => {
        expect(itemPageForTypeSelection(undefined, 20)).toEqual({
            selectedTypeId: undefined,
            page: 1,
            pageSize: 20,
            shouldLoad: false,
        })
    })

    it('selects a type when its table row is clicked', () => {
        const selected: number[] = []

        dictionaryTypeRow({ id: 7 }, (type) => selected.push(type.id)).onClick()

        expect(selected).toEqual([7])
    })
})
