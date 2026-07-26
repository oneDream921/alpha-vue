import { http, type ApiResponse } from './http'

export interface RedisOverview {
    redisVersion?: string
    uptimeSeconds?: number
    usedMemoryBytes?: number
    connectedClients?: number
    managedKeyCounts: Record<string, number>
}

export interface RedisKeyMetadata {
    key: string
    category: string
    type: string
    ttlSeconds?: number
    sizeBytes?: number
    valueRedacted: true
}

export interface RedisKeyPage {
    records: RedisKeyMetadata[]
    nextCursor: string
    hasMore: boolean
}

export interface RedisKeyQuery {
    prefix: string
    cursor: string
    count: number
}

export const redisApi = {
    overview: () =>
        http.get<ApiResponse<RedisOverview>>('/monitor/redis/overview'),
    keys: (query: RedisKeyQuery) =>
        http.get<ApiResponse<RedisKeyPage>>('/monitor/redis/keys', {
            params: query,
        }),
    key: (key: string) =>
        http.get<ApiResponse<RedisKeyMetadata>>('/monitor/redis/key', {
            params: { key },
        }),
    delete: (key: string) =>
        http.delete<ApiResponse<string>>('/monitor/redis/key', {
            params: { key },
        }),
}
