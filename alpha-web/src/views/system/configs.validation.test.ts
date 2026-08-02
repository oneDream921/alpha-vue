import { describe, expect, it } from 'vitest'

import type { ConfigDefinition } from '@/service/system/index'
import {
    validateConfigValue,
    validateDefinitionRules,
} from './configs.validation'

const definition = (
    overrides: Partial<ConfigDefinition>,
): ConfigDefinition => ({
    id: 1,
    configKey: 'file.example',
    configName: '测试配置',
    configGroup: 'file',
    valueType: 'STRING',
    sensitive: false,
    dynamic: false,
    status: 'PUBLISHED',
    enumValues: [],
    ...overrides,
})

describe('validateConfigValue', () => {
    it('validates Boolean, Integer, Enum and String definitions', () => {
        expect(
            validateConfigValue(definition({ valueType: 'BOOLEAN' }), 'true'),
        ).toBeUndefined()
        expect(
            validateConfigValue(definition({ valueType: 'BOOLEAN' }), 'TRUE'),
        ).toBeDefined()
        expect(
            validateConfigValue(
                definition({
                    valueType: 'INTEGER',
                    integerMin: 1,
                    integerMax: 3,
                }),
                '3',
            ),
        ).toBeUndefined()
        expect(
            validateConfigValue(
                definition({
                    valueType: 'INTEGER',
                    integerMin: 1,
                    integerMax: 3,
                }),
                '4',
            ),
        ).toBeDefined()
        expect(
            validateConfigValue(
                definition({ valueType: 'ENUM', enumValues: ['a', 'b'] }),
                'a',
            ),
        ).toBeUndefined()
        expect(
            validateConfigValue(
                definition({ valueType: 'ENUM', enumValues: ['a', 'b'] }),
                'c',
            ),
        ).toBeDefined()
        expect(
            validateConfigValue(
                definition({ stringMaxLength: 3, stringPattern: '[a-z]+' }),
                'abc',
            ),
        ).toBeUndefined()
        expect(
            validateConfigValue(
                definition({ stringMaxLength: 3, stringPattern: '[a-z]+' }),
                'ABCD',
            ),
        ).toBeDefined()
    })

    it('rejects incomplete or malformed definition rules', () => {
        expect(validateConfigValue(undefined, 'x')).toBeDefined()
        expect(
            validateDefinitionRules({ valueType: 'ENUM', defaultValue: 'a' }),
        ).toBeDefined()
        expect(
            validateDefinitionRules({
                valueType: 'INTEGER',
                defaultValue: '1',
                integerMin: 2,
                integerMax: 1,
            }),
        ).toBeDefined()
        expect(
            validateDefinitionRules({
                valueType: 'STRING',
                defaultValue: 'x',
                stringPattern: '(',
            }),
        ).toBeDefined()
    })
})
