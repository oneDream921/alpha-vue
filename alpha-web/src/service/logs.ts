import { http, type ApiResponse } from './http'
import type { PageResponse } from './system'

export interface OperationLog {
    id: number
    userId?: number
    username?: string
    module: string
    operation: string
    method?: string
    requestUri?: string
    responseCode?: number
    status: number
    ipAddress?: string
    location?: string
    clientId?: string
    deviceId?: string
    deviceName?: string
    browser?: string
    operatingSystem?: string
    errorCode?: number
    durationMs?: number
    traceId?: string
    handlingStatus: 0 | 1 | 2
    handledBy?: number
    handledAt?: string
    createdAt: string
}
export interface OperationLogDetail {
    summary: OperationLog
    exceptionStack?: string
    requestSummary?: string
    responseSummary?: string
}
export interface LoginLog {
    id: number
    username?: string
    userId?: number
    loginType: string
    status: number
    ipAddress?: string
    location?: string
    clientId?: string
    deviceId?: string
    deviceName?: string
    browser?: string
    operatingSystem?: string
    traceId?: string
    errorMessage?: string
    userAgent?: string
    message?: string
    createdAt: string
}
export interface OperationLogQuery {
    keyword?: string
    status?: 0 | 1
    handlingStatus?: 0 | 1 | 2
}
export const logApi = {
    operations: (page = 1, size = 10, query: OperationLogQuery = {}) =>
        http.get<ApiResponse<PageResponse<OperationLog>>>('/logs/operations', {
            params: { page, size, ...query },
        }),
    operationDetail: (id: number) =>
        http.get<ApiResponse<OperationLogDetail>>(`/logs/operations/${id}`),
    logins: (page = 1, size = 10) =>
        http.get<ApiResponse<PageResponse<LoginLog>>>('/logs/logins', {
            params: { page, size },
        }),
    updateHandlingStatus: (id: number, handlingStatus: 0 | 1 | 2) =>
        http.put<ApiResponse<null>>(`/logs/operations/${id}/handled`, null, {
            params: { handlingStatus },
        }),
}
