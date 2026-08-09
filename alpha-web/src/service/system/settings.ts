import { http, type ApiResponse } from '../http'

export type SettingGroup =
    | 'site'
    | 'login'
    | 'file'
    | 'oauth'
    | 'payment'
    | 'security'
    | 'mini_program'
    | 'official_account'

export interface SystemSetting {
    group: SettingGroup
    values: Record<string, unknown>
    secretConfigured: Record<string, boolean>
    restartRequired: boolean
}

export interface RsaKeyPair {
    publicKey: string
    privateKey: string
}

export interface FileStorageCredentials {
    accessKey: string | null
    secretKey: string | null
}

export interface PaymentSimulationOrder {
    id: number
    orderNo: string
    channel: 'WECHAT' | 'ALIPAY'
    amountFen: number
    status: 'PENDING' | 'SUCCEEDED' | 'FAILED'
    createdAt: string
}

export const systemSettingApi = {
    publicSettings() {
        return http.get<ApiResponse<Record<string, unknown>>>(
            '/system/settings/public',
        )
    },
    get(group: SettingGroup) {
        return http.get<ApiResponse<SystemSetting>>(`/system/settings/${group}`)
    },
    save(group: SettingGroup, values: Record<string, unknown>) {
        return http.put<ApiResponse<SystemSetting>>(
            `/system/settings/${group}`,
            { values },
        )
    },
    revealFileStorageCredentials() {
        return http.get<ApiResponse<FileStorageCredentials>>(
            '/system/settings/file/credentials',
        )
    },
    regenerateRsaKeys() {
        return http.post<ApiResponse<RsaKeyPair>>(
            '/system/settings/security/keys/regenerate',
        )
    },
    publishOfficialAccountMenu() {
        return http.post<null>('/system/settings/official-account/menu/publish')
    },
    createSimulationOrder(
        channel: 'wechat' | 'alipay',
        amountFen: number,
        idempotencyKey: string,
    ) {
        return http.post<ApiResponse<PaymentSimulationOrder>>(
            '/system/settings/pay/simulated-orders',
            { channel, amountFen, idempotencyKey },
        )
    },
    completeSimulationOrder(id: number, status: 'succeeded' | 'failed') {
        return http.post<ApiResponse<PaymentSimulationOrder>>(
            `/system/settings/pay/simulated-orders/${id}/complete`,
            { status },
        )
    },
}
