import type { NavigationGuard } from 'vue-router'

import { authStore } from '@/stores/auth'

export const authGuard: NavigationGuard = (to) => {
    if (to.meta.requiresAuth !== false && !authStore.getToken()) {
        return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (to.name === 'login' && authStore.getToken()) {
        return { name: 'dashboard' }
    }

    return true
}
