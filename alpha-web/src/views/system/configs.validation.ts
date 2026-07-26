const forbiddenPrefixes = [
    'spring.',
    'server.',
    'datasource.',
    'redis.',
    'minio.',
    'sa-token.',
]
const sensitiveSegment =
    /(^|[._-])(password|passwd|secret|token|credential|key|private[-_]?key|api[-_]?key|access[-_]?key)([._-]|$)/

export function validateConfigKey(value: string): string | undefined {
    const normalized = value.trim().toLowerCase()
    if (
        forbiddenPrefixes.some((prefix) => normalized.startsWith(prefix)) ||
        sensitiveSegment.test(normalized)
    ) {
        return '该配置键不允许保存'
    }
    return undefined
}
