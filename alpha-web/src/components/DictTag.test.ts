import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DictTag from './DictTag.vue'

const options = [
    { label: '草稿', value: 'draft', sortOrder: 1, isDefault: 1 },
    { label: '启用', value: 'enabled', sortOrder: 2, isDefault: 0 },
]

describe('DictTag', () => {
    it('renders matching labels and unmatched values', () => {
        const wrapper = mount(DictTag, {
            props: {
                options,
                value: 'draft,missing',
            },
            global: {
                stubs: {
                    'a-space': { template: '<div><slot /></div>' },
                    'a-tag': { template: '<span><slot /></span>' },
                },
            },
        })

        expect(wrapper.text()).toContain('草稿')
        expect(wrapper.text()).toContain('missing')
    })

    it('hides unmatched values when showValue is false', () => {
        const wrapper = mount(DictTag, {
            props: {
                options,
                value: 'missing',
                showValue: false,
            },
            global: {
                stubs: {
                    'a-space': { template: '<div><slot /></div>' },
                    'a-tag': { template: '<span><slot /></span>' },
                },
            },
        })

        expect(wrapper.text()).toBe('')
    })
})
