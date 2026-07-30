import { http, type ApiResponse } from './http'

export interface SqlLogEntry {
    id: number
    createdAt: string
    traceId?: string
    statementId: string
    sqlCommandType: string
    tableName?: string
    sql: string
    elapsedMs: number
    slow: boolean
    resultSize?: number
}

export interface SqlLogQuery {
    limit: number
    type?: string
    keyword?: string
    slowOnly: boolean
}

export interface SqlLogStatement {
    statementId: string
    mapperName: string
    methodName: string
}

export interface SqlLogSettings {
    enabled: boolean
    statements: SqlLogStatement[]
    excludedStatementIds: string[]
}

export interface SqlLogSettingsUpdate {
    enabled: boolean
    excludedStatementIds: string[]
}

export const sqlMonitorApi = {
    logs: (query: SqlLogQuery) =>
        http.get<ApiResponse<SqlLogEntry[]>>('/monitor/sql/logs', {
            params: query,
        }),
    clear: () => http.delete<ApiResponse<string>>('/monitor/sql/logs'),
    settings: () =>
        http.get<ApiResponse<SqlLogSettings>>('/monitor/sql/settings'),
    updateSettings: (body: SqlLogSettingsUpdate) =>
        http.put<ApiResponse<SqlLogSettings>>('/monitor/sql/settings', body),
}
