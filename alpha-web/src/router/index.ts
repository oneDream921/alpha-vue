import { h } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'

import BaseLayout from '@/layouts/BaseLayout.vue'
import { authGuard } from './guard'

const PlaceholderPage = (title: string) => ({
    name: `${title}Placeholder`,
    render: () => h('section', { class: 'page-placeholder' }, [h('h1', title)]),
})

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: PlaceholderPage('登录'),
            meta: { requiresAuth: false },
        },
        {
            path: '/',
            component: BaseLayout,
            children: [
                {
                    path: '',
                    name: 'dashboard',
                    component: PlaceholderPage('工作台'),
                },
                {
                    path: 'profile',
                    name: 'profile',
                    component: PlaceholderPage('个人中心'),
                },
            ],
        },
    ],
})

router.beforeEach(authGuard)

export default router
