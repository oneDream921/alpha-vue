import { describe, expect, it } from 'vitest'

import { validateConfigKey } from './configs.validation'

describe('validateConfigKey', () => {
    it('rejects infrastructure and sensitive configuration keys', () => {
        expect(validateConfigKey('spring.application.name')).toBe(
            '该配置键不允许保存',
        )
        expect(validateConfigKey('app.api-token')).toBe('该配置键不允许保存')
    })

    it('accepts ordinary application configuration keys', () => {
        expect(validateConfigKey('app.home.notice')).toBeUndefined()
    })
})
