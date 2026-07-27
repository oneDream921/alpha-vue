import { describe, expect, it, vi } from 'vitest'

const { get, remove } = vi.hoisted(() => ({ get: vi.fn(), remove: vi.fn() }))

vi.mock('./http', () => ({
    http: { get, delete: remove },
}))

import { sqlMonitorApi } from './sqlMonitor'

describe('sqlMonitorApi', () => {
    it('queries recent SQL logs with filters', () => {
        sqlMonitorApi.logs({
            limit: 100,
            type: 'SELECT',
            keyword: 'sys_user',
            slowOnly: true,
        })

        expect(get).toHaveBeenCalledWith('/monitor/sql/logs', {
            params: {
                limit: 100,
                type: 'SELECT',
                keyword: 'sys_user',
                slowOnly: true,
            },
        })
    })

    it('uses the protected clear endpoint', () => {
        sqlMonitorApi.clear()

        expect(remove).toHaveBeenCalledWith('/monitor/sql/logs')
    })
})
