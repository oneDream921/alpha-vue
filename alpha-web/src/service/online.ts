import { http, type ApiResponse } from './http'
import type { PageResponse } from './system'

export interface OnlineSession {
    userId: number
    terminalIndex: number
    tokenSummary: string
    clientId: string
    deviceId?: string
    deviceName?: string
    ipAddress?: string
    browser: string
    operatingSystem: string
    loginTime: string
    lastActiveTime: string
    timeoutSeconds: number
    user?: {
        id: number
        username: string
        nickname: string
        deptName?: string
    }
}

export const onlineApi = {
    page: (page = 1, size = 10) =>
        http.get<ApiResponse<PageResponse<OnlineSession>>>(
            '/monitor/online-users',
            {
                params: { page, size },
            },
        ),
    kickout: (userId: number, terminalIndex: number) =>
        http.delete<ApiResponse<null>>(
            `/monitor/online-users/${userId}/sessions/${terminalIndex}`,
        ),
}
