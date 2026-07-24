import { http, type ApiResponse } from './http'
import type { AppRoute, UserProfile } from '@/stores/auth'

export interface LoginPayload {
    username: string
    password: string
    captcha?: string
    captchaId?: string
}

export interface LoginResult {
    token: string
}

export const authApi = {
    login: (payload: LoginPayload) =>
        http.post<ApiResponse<LoginResult>>('/auth/login', payload),
    logout: () => http.post<ApiResponse<null>>('/auth/logout'),
    profile: () => http.get<ApiResponse<UserProfile>>('/auth/profile'),
    routes: () => http.get<ApiResponse<AppRoute[]>>('/auth/routes'),
}
