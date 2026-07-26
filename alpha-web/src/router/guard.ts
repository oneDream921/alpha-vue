import type { NavigationGuard, RouteLocationRaw } from 'vue-router'
import { Modal } from 'ant-design-vue'

import { authStore } from '@/stores/auth'

let loginPromptOpen = false

function requestLogin(redirect: string): Promise<false | RouteLocationRaw> {
    if (loginPromptOpen) {
        return Promise.resolve(false)
    }

    loginPromptOpen = true
    return new Promise((resolve) => {
        Modal.warning({
            title: '请先登录',
            content: '访问该页面需要登录账号。',
            okText: '前往登录',
            closable: false,
            maskClosable: false,
            onOk: () => {
                loginPromptOpen = false
                resolve({ name: 'login', query: { redirect } })
            },
        })
    })
}

export const authGuard: NavigationGuard = (to) => {
    if (to.meta.requiresAuth !== false && !authStore.getToken()) {
        return requestLogin(to.fullPath)
    }

    if (to.name === 'login' && authStore.getToken()) {
        return { name: 'dashboard' }
    }

    const permission = to.meta.permission
    if (
        typeof permission === 'string' &&
        !authStore.hasPermission(permission)
    ) {
        return { name: 'forbidden' }
    }

    return true
}
