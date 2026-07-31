import { describe, expect, it } from 'vitest'
import { formatDateTime, parseDateTime } from './dateTime'

describe('dateTime', () => {
    it('treats backend LocalDateTime as local time', () => {
        const date = parseDateTime('2026-07-31 19:32:37')
        expect(date.getHours()).toBe(19)
        expect(date.getMinutes()).toBe(32)
    })

    it('keeps explicit timezone timestamps timezone-aware', () => {
        expect(parseDateTime('2026-07-31T11:32:37Z').toISOString()).toBe(
            '2026-07-31T11:32:37.000Z',
        )
    })

    it('formats invalid values as a placeholder', () => {
        expect(formatDateTime('invalid')).toBe('-')
    })
})
