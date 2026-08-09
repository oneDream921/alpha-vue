import axios, { type AxiosInstance } from 'axios'
import { message, Modal } from 'ant-design-vue'

import { authStore, type AuthStore } from '@/stores/auth'

declare module 'axios' {
    interface AxiosRequestConfig {
        suppressGlobalErrorMessage?: boolean
    }
}

export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    traceId: string
}

let loginPromptOpen = false

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
            if (axios.isAxiosError(error)) {
                if (error.response?.status === 401) {
                    store.clearAuth()
                    void import('@/router').then(({ default: router }) => {
                        if (
                            loginPromptOpen ||
                            router.currentRoute.value.name === 'login'
                        ) {
                            return
                        }
                        loginPromptOpen = true
                        Modal.warning({
                            title: '需要登录',
                            content: '当前登录状态已失效，请重新登录。',
                            okText: '去登录',
                            closable: false,
                            maskClosable: false,
                            onOk: () => {
                                loginPromptOpen = false
                                return router.replace({
                                    name: 'login',
                                    query: {
                                        redirect:
                                            router.currentRoute.value.fullPath,
                                    },
                                })
                            },
                        })
                    })
                }
                const requestUrl = error.config?.url ?? ''
                const requestPath = requestUrl.split('?', 1)[0]
                const isLoginRequest =
                    error.config?.method?.toLowerCase() === 'post' &&
                    requestPath === '/auth/login'
                if (
                    error.response?.status !== 401 &&
                    !isLoginRequest &&
                    !error.config?.suppressGlobalErrorMessage
                ) {
                    const errorMessage = error.response?.data?.message
                    message.error(
                        typeof errorMessage === 'string' && errorMessage
                            ? errorMessage
                            : '请求失败，请确认后端服务已启动后重试',
                    )
                }
            }
            return Promise.reject(error)
        },
    )

    return client
}

export const http = createHttpClient(authStore)
