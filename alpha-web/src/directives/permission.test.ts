import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'

import { permission } from './permission'
import { authStore } from '@/stores/auth'

describe('v-permission', () => {
    beforeEach(() => {
        authStore.clearAuth()
        authStore.setProfile({
            id: 2,
            username: 'operator',
            roles: [],
            permissions: ['system:user:list'],
            mustChangePassword: false,
        })
    })

    it('removes a button when its permission is absent', () => {
        const wrapper = mount(
            {
                template:
                    '<div><button v-permission="\'system:user:create\'">新建</button></div>',
            },
            { global: { directives: { permission } } },
        )

        expect(wrapper.find('button').exists()).toBe(false)
    })

    it('keeps a button when its permission is present', () => {
        const wrapper = mount(
            {
                template:
                    '<div><button v-permission="\'system:user:list\'">查询</button></div>',
            },
            { global: { directives: { permission } } },
        )

        expect(wrapper.find('button').exists()).toBe(true)
    })
})
