import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RedisTrendChart from './RedisTrendChart.vue'

describe('RedisTrendChart', () => {
    it('renders Redis Long values serialized as strings', () => {
        const wrapper = mount(RedisTrendChart, {
            props: {
                title: '内存趋势',
                metric: 'usedMemoryBytes',
                points: [
                    {
                        capturedAt: '2026-08-01T00:00:00Z',
                        usedMemoryBytes: '2056240',
                    },
                    {
                        capturedAt: '2026-08-01T00:01:00Z',
                        usedMemoryBytes: '3072000',
                    },
                ],
            },
        })

        expect(wrapper.text()).toContain('2 个有效点')
        expect(wrapper.text()).toContain('当前 3,072,000')
        expect(wrapper.text()).toContain('2,056,240')
        expect(
            wrapper.find('[data-testid="redis-trend-echart"]').exists(),
        ).toBe(true)
    })

    it('keeps invalid numeric strings out of the chart', () => {
        const wrapper = mount(RedisTrendChart, {
            props: {
                title: '连接趋势',
                metric: 'connectedClients',
                points: [
                    {
                        capturedAt: '2026-08-01T00:00:00Z',
                        connectedClients: 'not-a-number',
                    },
                ],
            },
        })

        expect(wrapper.text()).toContain('0 个有效点')
        expect(
            wrapper.find('[data-testid="redis-trend-echart"]').exists(),
        ).toBe(false)
    })
})
