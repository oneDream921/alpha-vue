import Antd from 'ant-design-vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { overview, metrics, keys } = vi.hoisted(() => ({
    overview: vi.fn(),
    metrics: vi.fn(),
    keys: vi.fn(),
}))

vi.mock('@/service/redis', () => ({
    redisApi: {
        overview,
        metrics,
        keys,
        key: vi.fn(),
        delete: vi.fn(),
    },
}))

vi.mock('ant-design-vue', async () => {
    const actual =
        await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
    return {
        ...actual,
        message: {
            error: vi.fn(),
            success: vi.fn(),
        },
    }
})

import Redis from './redis.vue'

describe('Redis management page metrics isolation', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        overview.mockResolvedValue({
            data: {
                data: {
                    redisVersion: '7.2',
                    usedMemoryBytes: 100,
                    connectedClients: 2,
                    uptimeSeconds: 60,
                    managedKeyCounts: { '全部 Redis 键': 1 },
                },
            },
        })
        metrics.mockRejectedValue(new Error('metrics unavailable'))
        keys.mockResolvedValue({
            data: {
                data: {
                    records: [
                        {
                            key: 'cache:one',
                            category: '业务缓存',
                            type: 'string',
                            valueTruncated: false,
                        },
                    ],
                    nextCursor: '0',
                    hasMore: false,
                },
            },
        })
    })

    it('keeps overview and bounded key scan usable when metrics fail', async () => {
        const wrapper = mount(Redis, {
            global: {
                plugins: [Antd],
                stubs: {
                    TableActionMenu: {
                        template: '<div />',
                    },
                    'a-table': {
                        template: '<div />',
                    },
                    'a-table-column': {
                        template: '<div />',
                    },
                },
                directives: {
                    permission: () => undefined,
                },
            },
        })

        await flushPromises()

        expect(metrics).toHaveBeenCalledOnce()
        expect(overview).toHaveBeenCalledOnce()
        expect(keys).toHaveBeenCalledOnce()
        expect(wrapper.text()).toContain('Redis 版本')
        expect(wrapper.text()).toContain('Redis 指标加载失败')
    })

    it('renders the current, command, and trend fields returned by the metrics API', async () => {
        metrics.mockResolvedValue({
            data: {
                data: {
                    enabled: true,
                    status: 'HEALTHY',
                    consecutiveFailures: 0,
                    sampleIntervalSeconds: 60,
                    retentionHours: 24,
                    maxSamples: 1440,
                    current: {
                        usedMemoryBytes: 1024,
                        usedMemoryRssBytes: 2048,
                        connectedClients: 3,
                        blockedClients: 0,
                        instantaneousOpsPerSecond: 12,
                        memoryFragmentationRatio: 1.25,
                        maxMemoryBytes: 0,
                        totalSystemMemoryBytes: 4096,
                        totalCommandsProcessed: 20,
                    },
                    commands: [
                        {
                            command: 'get',
                            calls: '10',
                            callsPerSecond: 0.16666666666666666,
                            failedCalls: '0',
                            usecPerCall: 2.5,
                        },
                    ],
                    trend: [
                        {
                            capturedAt: '2026-08-01T00:00:00Z',
                            usedMemoryBytes: 1024,
                            connectedClients: 3,
                            instantaneousOpsPerSecond: 12,
                        },
                    ],
                },
            },
        })

        const wrapper = mount(Redis, {
            global: {
                plugins: [Antd],
                stubs: {
                    TableActionMenu: { template: '<div />' },
                    RedisTrendChart: { template: '<div />' },
                },
                directives: {
                    permission: () => undefined,
                },
            },
        })

        await flushPromises()

        expect(wrapper.text()).toContain('采集中')
        expect(wrapper.text()).toContain('1 KB')
        expect(wrapper.text()).toContain('2 KB')
        expect(wrapper.text()).toContain('运行概览')
        expect(wrapper.text()).toContain('趋势分析')
        expect(wrapper.text()).toContain('键管理')
        expect(wrapper.text()).toContain('Redis 基本信息')
        expect(wrapper.text()).toContain('内存信息')
        expect(wrapper.text()).toContain('计算分母 系统内存 4.00 KB')
        expect(wrapper.text()).toContain('命令统计')
        expect(wrapper.text()).toContain('get')
        expect(wrapper.text()).toContain('0.17')
        expect(wrapper.text()).toContain('2.50')
        expect(wrapper.text()).not.toContain('0.16666666666666666')
        expect(
            wrapper
                .findAll('.redis-kpi-value')
                .some((node) => node.attributes('title') === '1 KB'),
        ).toBe(true)
        expect(
            wrapper
                .findAll('.redis-basic-info-value')
                .some((node) => node.attributes('title') === '1 KB'),
        ).toBe(true)
    })
})
