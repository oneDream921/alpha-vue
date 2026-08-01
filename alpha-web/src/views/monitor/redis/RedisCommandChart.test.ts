import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RedisCommandChart from './RedisCommandChart.vue'

describe('RedisCommandChart', () => {
    it('renders command percentages and other commands from the total count', () => {
        const wrapper = mount(RedisCommandChart, {
            props: {
                totalCalls: '100',
                commands: [
                    { command: 'get', calls: '60' },
                    { command: 'set', calls: 25 },
                ],
            },
        })

        expect(wrapper.text()).toContain('100')
        expect(wrapper.text()).toContain('get')
        expect(wrapper.text()).toContain('60.00%')
        expect(wrapper.text()).toContain('set')
        expect(wrapper.text()).toContain('其他命令')
        expect(
            wrapper.find('[data-testid="redis-command-echart"]').exists(),
        ).toBe(true)
    })

    it('shows an empty state when there are no command calls', () => {
        const wrapper = mount(RedisCommandChart, {
            props: {
                totalCalls: 0,
                commands: [],
            },
        })

        expect(wrapper.text()).toContain('暂无命令统计')
    })
})
