<script setup lang="ts">
import {
    LockOutlined,
    ReloadOutlined,
    UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '@/service/auth'
import { clearManagementRoutes, ensureManagementRoutes } from '@/router'
import { authStore } from '@/stores/auth'
import logoUrl from '@/assets/alpha-logo.svg'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({
    username: '',
    password: '',
    clientId: 'pc-admin',
    deviceId: '',
    deviceName: '',
    rememberMe: false,
    captchaId: '',
    captcha: '',
})
const captchaEnabled = ref(false)
const captchaImage = ref<string>()

async function loadCaptcha() {
    const response = await authApi.captcha()
    captchaEnabled.value = response.data.data.enabled
    captchaImage.value = response.data.data.image ?? undefined
    form.captchaId = response.data.data.captchaId ?? ''
    form.captcha = ''
}

async function submit() {
    loading.value = true
    authStore.clearAuth()
    clearManagementRoutes()
    try {
        const login = await authApi.login(form)
        authStore.setToken(login.data.data.token)
        const [profile, routes] = await Promise.all([
            authApi.profile(),
            authApi.routes(),
        ])
        authStore.setSession(
            login.data.data.token,
            profile.data.data,
            routes.data.data,
        )
        ensureManagementRoutes(routes.data.data)
        const redirect =
            typeof route.query.redirect === 'string'
                ? route.query.redirect
                : '/'
        await router.replace(redirect)
    } catch {
        authStore.clearAuth()
        clearManagementRoutes()
        message.error('登录失败，请检查账号和密码')
        if (captchaEnabled.value) await loadCaptcha()
    } finally {
        loading.value = false
    }
}
onMounted(loadCaptcha)
</script>

<template>
    <main class="login-page">
        <div class="login-environment" aria-hidden="true">
            <img class="login-environment-mark" :src="logoUrl" alt="" />
            <strong>Alpha Vue</strong>
            <span>简洁可靠的后台管理基础框架</span>
        </div>
        <section class="login-panel" aria-labelledby="login-title">
            <div class="login-heading">
                <img class="login-brand" :src="logoUrl" alt="Alpha Vue" />
                <div>
                    <h1 id="login-title">欢迎登录</h1>
                    <p>使用管理账号进入 Alpha Vue</p>
                </div>
            </div>
            <a-form :model="form" layout="vertical" @finish="submit">
                <a-form-item
                    label="账号"
                    name="username"
                    :rules="[{ required: true, message: '请输入账号' }]"
                >
                    <a-input
                        v-model:value="form.username"
                        autocomplete="username"
                        size="large"
                    >
                        <template #prefix><UserOutlined /></template>
                    </a-input>
                </a-form-item>
                <a-form-item
                    label="密码"
                    name="password"
                    :rules="[{ required: true, message: '请输入密码' }]"
                >
                    <a-input-password
                        v-model:value="form.password"
                        autocomplete="current-password"
                        size="large"
                    >
                        <template #prefix><LockOutlined /></template>
                    </a-input-password>
                </a-form-item>
                <a-form-item
                    v-if="captchaEnabled"
                    label="验证码"
                    name="captcha"
                    :rules="[
                        {
                            required: true,
                            whitespace: true,
                            message: '请输入验证码',
                        },
                    ]"
                >
                    <div class="captcha-row">
                        <a-input
                            v-model:value="form.captcha"
                            :maxlength="16"
                        /><a-button
                            class="captcha-button"
                            title="刷新验证码"
                            aria-label="刷新验证码"
                            @click="loadCaptcha"
                            ><img
                                v-if="captchaImage"
                                :src="captchaImage"
                                alt="验证码" /><ReloadOutlined v-else
                        /></a-button>
                    </div>
                </a-form-item>
                <a-form-item
                    ><a-checkbox v-model:checked="form.rememberMe"
                        >7 天内保持登录</a-checkbox
                    ></a-form-item
                >
                <a-button
                    type="primary"
                    html-type="submit"
                    size="large"
                    block
                    :loading="loading"
                >
                    登录
                </a-button>
            </a-form>
        </section>
    </main>
</template>
