import type { ConfigDefinition } from '@/service/system'

export function validateConfigValue(
    definition: ConfigDefinition | undefined,
    value: string,
): string | undefined {
    if (!definition) return '请选择已发布的配置项'
    const normalized = value.trim()
    if (definition.valueType === 'BOOLEAN') {
        return normalized === 'true' || normalized === 'false'
            ? undefined
            : '布尔值只能填写 true 或 false'
    }
    if (definition.valueType === 'INTEGER') {
        const number = Number(normalized)
        if (!Number.isInteger(number)) return '请输入整数'
        if (
            (definition.integerMin !== undefined &&
                number < definition.integerMin) ||
            (definition.integerMax !== undefined &&
                number > definition.integerMax)
        ) {
            return `整数必须在 ${definition.integerMin ?? '-∞'} 至 ${definition.integerMax ?? '+∞'} 之间`
        }
        return undefined
    }
    if (definition.valueType === 'ENUM') {
        return definition.enumValues.includes(value)
            ? undefined
            : '请输入定义目录中的枚举值'
    }
    if (
        definition.stringMaxLength !== undefined &&
        value.length > definition.stringMaxLength
    ) {
        return `文本不能超过 ${definition.stringMaxLength} 个字符`
    }
    if (definition.stringPattern) {
        try {
            return new RegExp(`^(?:${definition.stringPattern})$`).test(value)
                ? undefined
                : '文本格式不符合定义规则'
        } catch {
            return '定义的文本规则无效'
        }
    }
    return undefined
}

export function validateDefinitionRules(value: {
    valueType: ConfigDefinition['valueType']
    defaultValue: string
    integerMin?: number
    integerMax?: number
    stringMaxLength?: number
    stringPattern?: string
    enumValues?: string
}): string | undefined {
    if (!value.defaultValue.trim()) return '请输入默认值'
    if (value.valueType === 'INTEGER') {
        if (
            value.integerMin !== undefined &&
            value.integerMax !== undefined &&
            value.integerMin > value.integerMax
        ) {
            return '整数最小值不能大于最大值'
        }
    }
    if (value.valueType === 'ENUM') {
        if (!value.enumValues?.trim()) return '枚举类型必须填写可选值'
        if (value.enumValues.split(',').some((item) => !item.trim())) {
            return '枚举值不能包含空项'
        }
    }
    if (value.valueType === 'STRING' && value.stringPattern) {
        try {
            new RegExp(value.stringPattern)
        } catch {
            return '文本正则规则无效'
        }
    }
    return undefined
}
