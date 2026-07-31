import axios, { type AxiosInstance } from 'axios'
import { message, Modal } from 'ant-design-vue'

import { authStore, type AuthStore } from '@/stores/auth'

export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    traceId: string
}

let unauthorizedPromptOpen = false

function promptLogin() {
    if (unauthorizedPromptOpen) {
        return
    }

    unauthorizedPromptOpen = true
    Modal.confirm({
        title: '请先登录',
        content: '当前登录已失效，请重新登录后继续操作。',
        okText: '前往登录',
        cancelText: '取消',
        okType: 'primary',
        closable: false,
        maskClosable: false,
        onOk: async () => {
            unauthorizedPromptOpen = false
            const { default: router } = await import('@/router')
            await router.replace({
                name: 'login',
                query: { redirect: router.currentRoute.value.fullPath },
            })
        },
        onCancel: () => {
            unauthorizedPromptOpen = false
        },
    })
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
            if (axios.isAxiosError(error)) {
                if (error.response?.status === 401) {
                    store.clearAuth()
                    promptLogin()
                }
                const errorMessage = error.response?.data?.message
                message.error(
                    typeof errorMessage === 'string' && errorMessage
                        ? errorMessage
                        : '请求失败，请确认后端服务已启动后重试',
                )
            }
            return Promise.reject(error)
        },
    )

    return client
}

export const http = createHttpClient(authStore)
