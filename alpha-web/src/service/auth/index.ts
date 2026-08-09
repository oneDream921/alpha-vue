import { http, type ApiResponse } from '../http'
import type { AppRoute, UserProfile } from '@/stores/auth'

export interface LoginPayload {
    username: string
    password: string
    deviceId?: string
    deviceName?: string
    captcha?: string
    captchaId?: string
    rememberMe?: boolean
}

export interface LoginResult {
    token: string
    tokenType: string
    expiresIn: number
}

export interface CaptchaResult {
    enabled: boolean
    type: 'numeric' | 'slider'
    rememberMeEnabled: boolean
    captchaId: string | null
    image: string | null
    question: string | null
    sliderBackground: string | null
    sliderPiece: string | null
    sliderWidth: number | null
    sliderHeight: number | null
    sliderPieceWidth: number | null
    sliderPieceTop: number | null
}

export const authApi = {
    captcha: () => http.get<ApiResponse<CaptchaResult>>('/auth/captcha'),
    login: (payload: LoginPayload) =>
        http.post<ApiResponse<LoginResult>>('/auth/login', {
            ...payload,
            clientId: 'pc-admin',
        }),
    logout: () => http.post<ApiResponse<null>>('/auth/logout'),
    profile: () => http.get<ApiResponse<UserProfile>>('/auth/profile'),
    routes: () => http.get<ApiResponse<AppRoute[]>>('/auth/routes'),
    updateProfile: (payload: {
        nickname: string
        avatar?: string
        email?: string
        phone?: string
    }) => http.put<ApiResponse<UserProfile>>('/auth/profile', payload),
    uploadAvatar: (file: File) => {
        const data = new FormData()
        data.append('file', file)
        return http.post<ApiResponse<UserProfile>>('/auth/avatar', data)
    },
    changePassword: (payload: {
        currentPassword: string
        newPassword: string
    }) => http.put<ApiResponse<null>>('/auth/password', payload),
}
