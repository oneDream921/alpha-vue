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
    value?: string
    valueTruncated: boolean
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
    keyword?: string
}

export type RedisMetricsStatus =
    'DISABLED' | 'COLLECTING' | 'HEALTHY' | 'DEGRADED' | 'STALE'

export interface RedisMetricsSnapshot {
    redisVersion?: string
    uptimeSeconds?: RedisMetricValue
    usedMemoryBytes?: RedisMetricValue
    usedMemoryRssBytes?: RedisMetricValue
    usedMemoryPeakBytes?: RedisMetricValue
    maxMemoryBytes?: RedisMetricValue
    totalSystemMemoryBytes?: RedisMetricValue
    memoryFragmentationRatio?: number
    connectedClients?: RedisMetricValue
    blockedClients?: RedisMetricValue
    totalConnectionsReceived?: RedisMetricValue
    rejectedConnections?: RedisMetricValue
    totalCommandsProcessed?: RedisMetricValue
    instantaneousOpsPerSecond?: RedisMetricValue
    keyspaceHits?: RedisMetricValue
    keyspaceMisses?: RedisMetricValue
}

export interface RedisCommandMetric {
    command: string
    calls?: RedisMetricValue
    intervalCalls?: RedisMetricValue
    callsPerSecond?: number
    rejectedCalls?: RedisMetricValue
    failedCalls?: RedisMetricValue
    usecPerCall?: number
}

export type RedisMetricValue = number | string

export interface RedisMetricsTrendPoint {
    capturedAt: string
    usedMemoryBytes?: RedisMetricValue | null
    usedMemoryRssBytes?: RedisMetricValue | null
    connectedClients?: RedisMetricValue | null
    blockedClients?: RedisMetricValue | null
    instantaneousOpsPerSecond?: RedisMetricValue | null
    totalCommandsProcessed?: RedisMetricValue | null
    rejectedConnections?: RedisMetricValue | null
}

export interface RedisMetrics {
    enabled: boolean
    status: RedisMetricsStatus
    lastAttemptAt?: string
    lastSuccessAt?: string
    consecutiveFailures: number
    sampleIntervalSeconds: number
    retentionHours: number
    maxSamples: number
    current?: RedisMetricsSnapshot
    commands: RedisCommandMetric[]
    trend: RedisMetricsTrendPoint[]
}

export const redisApi = {
    overview: () =>
        http.get<ApiResponse<RedisOverview>>('/monitor/redis/overview'),
    metrics: () =>
        http.get<ApiResponse<RedisMetrics>>('/monitor/redis/metrics'),
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
