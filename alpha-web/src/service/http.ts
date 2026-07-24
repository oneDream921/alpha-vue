import axios, { type AxiosInstance } from 'axios'

import { authStore, type AuthStore } from '@/stores/auth'

export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    traceId: string
}

export function createHttpClient(store: AuthStore): AxiosInstance {
    const client = axios.create({
        baseURL: '/api',
        timeout: 10_000,
    })

    client.interceptors.request.use((config) => {
        const token = store.getToken()
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    })

    client.interceptors.response.use(
        (response) => response,
        (error: unknown) => {
            if (axios.isAxiosError(error) && error.response?.status === 401) {
                store.clearAuth()
            }
            return Promise.reject(error)
        },
    )

    return client
}

export const http = createHttpClient(authStore)
