import { describe, expect, it } from 'vitest'

import { toggleDeptSelection } from './users.dept-selection'

describe('user department selection', () => {
    it('selects a department from a tree key', () => {
        expect(toggleDeptSelection(undefined, ['12'])).toBe(12)
    })

    it('clears the active department when the same node is clicked again', () => {
        expect(toggleDeptSelection(12, [12])).toBeUndefined()
    })

    it('clears selection when the tree emits no selected key', () => {
        expect(toggleDeptSelection(12, [])).toBeUndefined()
    })
})
