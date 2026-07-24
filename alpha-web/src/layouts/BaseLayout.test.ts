import Antd from 'ant-design-vue'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'

import BaseLayout from './BaseLayout.vue'

describe('BaseLayout', () => {
    afterEach(() => {
        window.innerWidth = 1024
        window.dispatchEvent(new Event('resize'))
    })

    it('renders drawer navigation below the mobile breakpoint', async () => {
        window.innerWidth = 767
        const wrapper = mount(BaseLayout, {
            global: {
                plugins: [Antd],
                stubs: {
                    RouterLink: { template: '<a><slot /></a>' },
                    RouterView: { template: '<div />' },
                },
            },
        })

        await wrapper.get('[data-testid="mobile-nav-trigger"]').trigger('click')

        expect(
            document.body.querySelector('[data-testid="mobile-navigation"]'),
        ).not.toBeNull()
        expect(document.body.textContent).toContain('工作台')
    })
})
