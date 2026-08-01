import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RedisMemoryGauge from './RedisMemoryGauge.vue'

describe('RedisMemoryGauge', () => {
    it('renders memory usage with a bounded utilization percent', () => {
        const wrapper = mount(RedisMemoryGauge, {
            props: {
                usedBytes: '1024',
                maxBytes: '2048',
                fragmentationRatio: 1.25,
            },
        })

        expect(wrapper.text()).toContain('1.00 KB')
        expect(wrapper.text()).toContain('使用率 50.00%')
        expect(wrapper.text()).toContain('最大内存 2.00 KB')
        expect(wrapper.text()).toContain('碎片率 1.25')
        expect(
            wrapper.find('[data-testid="redis-memory-gauge"]').exists(),
        ).toBe(true)
        expect(
            wrapper.find('[data-testid="redis-memory-echart"]').exists(),
        ).toBe(true)
    })

    it('does not invent a utilization percent when maxmemory is not configured', () => {
        const wrapper = mount(RedisMemoryGauge, {
            props: {
                usedBytes: 1024,
                maxBytes: 0,
                totalSystemBytes: 4096,
            },
        })

        expect(wrapper.text()).toContain('使用率 25.00%')
        expect(wrapper.text()).toContain('最大内存 未配置')
        expect(wrapper.text()).toContain('计算分母 系统内存 4.00 KB')
        expect(
            wrapper.find('[data-testid="redis-memory-echart"]').exists(),
        ).toBe(true)
    })
})
