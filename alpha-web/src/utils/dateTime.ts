const LOCAL_DATE_TIME_PATTERN =
    /^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2})(?:\.\d+)?$/

export function parseDateTime(value: string): Date {
    return new Date(value.replace(LOCAL_DATE_TIME_PATTERN, '$1T$2'))
}

export function formatDateTime(value?: string): string {
    if (!value) return '-'
    const date = parseDateTime(value)
    return Number.isNaN(date.getTime())
        ? '-'
        : date.toLocaleString('zh-CN', { hour12: false })
}
