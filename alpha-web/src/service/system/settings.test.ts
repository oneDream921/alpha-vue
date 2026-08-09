import { describe, expect, it, vi } from 'vitest'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('../http', () => ({
    http: { post },
}))

import { systemSettingApi } from './settings'

describe('systemSettingApi', () => {
    it('regenerates RSA keys through the protected settings endpoint', () => {
        systemSettingApi.regenerateRsaKeys()

        expect(post).toHaveBeenCalledWith(
            '/system/settings/security/keys/regenerate',
        )
    })

    it('publishes the configured official-account menu', () => {
        systemSettingApi.publishOfficialAccountMenu()

        expect(post).toHaveBeenCalledWith(
            '/system/settings/official-account/menu/publish',
        )
    })
})
