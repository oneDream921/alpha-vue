<script setup lang="ts">
import {
    CloseCircleOutlined,
    HolderOutlined,
    InfoCircleOutlined,
    LockOutlined,
    ReloadOutlined,
    UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import axios from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '@/service/auth/index'
import { clearManagementRoutes, ensureManagementRoutes } from '@/router'
import { authStore } from '@/stores/auth'
import logoUrl from '@/assets/alpha-logo.svg'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({
    username: '',
    password: '',
    deviceId: '',
    deviceName: '',
    rememberMe: false,
    captchaId: '',
    captcha: '',
})
const captchaEnabled = ref(false)
const captchaType = ref<'numeric' | 'slider'>('numeric')
const rememberMeEnabled = ref(true)
const captchaImage = ref<string>()
const captchaQuestion = ref<string>()
const sliderBackground = ref<string>()
const sliderPiece = ref<string>()
const sliderWidth = ref(420)
const sliderHeight = ref(280)
const sliderPieceWidth = ref(42)
const sliderPieceTop = ref(42)
const sliderOffset = ref(0)
const sliderDragging = ref(false)
const sliderStartX = ref(0)
const sliderModalOpen = ref(false)
const sliderTrack = ref<{
    getBoundingClientRect: () => { left: number; top: number }
}>()
const sliderTrace = ref<Array<{ x: number; y: number; t: number }>>([])
const sliderStartedAt = ref(0)
const sliderStatus = ref('')

async function loadCaptcha() {
    const response = await authApi.captcha()
    captchaEnabled.value = response.data.data.enabled
    captchaType.value = response.data.data.type ?? 'numeric'
    rememberMeEnabled.value = response.data.data.rememberMeEnabled ?? true
    if (!rememberMeEnabled.value) form.rememberMe = false
    captchaImage.value = response.data.data.image ?? undefined
    captchaQuestion.value = response.data.data.question ?? undefined
    sliderBackground.value = response.data.data.sliderBackground ?? undefined
    sliderPiece.value = response.data.data.sliderPiece ?? undefined
    sliderWidth.value = response.data.data.sliderWidth ?? 420
    sliderHeight.value = response.data.data.sliderHeight ?? 280
    sliderPieceWidth.value = response.data.data.sliderPieceWidth ?? 42
    sliderPieceTop.value = response.data.data.sliderPieceTop ?? 42
    sliderOffset.value = 0
    sliderTrace.value = []
    sliderStartedAt.value = 0
    sliderStatus.value = ''
    form.captchaId = response.data.data.captchaId ?? ''
    form.captcha = ''
}

type SliderPointerEvent = {
    clientX: number
    clientY: number
    currentTarget: unknown
    pointerId: number
}

function startSlider(event: SliderPointerEvent) {
    if (!sliderPiece.value) return
    sliderStatus.value = '正在验证'
    sliderDragging.value = true
    sliderStartedAt.value = Date.now()
    sliderStartX.value = event.clientX - sliderOffset.value
    sliderTrace.value = []
    recordSliderPoint(event)
    ;(
        event.currentTarget as {
            setPointerCapture: (pointerId: number) => void
        }
    ).setPointerCapture(event.pointerId)
}

function moveSlider(event: SliderPointerEvent) {
    if (!sliderDragging.value) return
    const maxOffset = Math.max(0, sliderWidth.value - sliderPieceWidth.value)
    sliderOffset.value = Math.min(
        maxOffset,
        Math.max(0, event.clientX - sliderStartX.value),
    )
    recordSliderPoint(event)
}

function recordSliderPoint(
    event: SliderPointerEvent,
    elapsed = Date.now() - sliderStartedAt.value,
    force = false,
) {
    const bounds = sliderTrack.value?.getBoundingClientRect()
    if (!bounds) return
    const y = Math.max(0, Math.min(48, event.clientY - bounds.top))
    const last = sliderTrace.value.at(-1)
    if (!last || force || elapsed - last.t >= 16) {
        sliderTrace.value.push({ x: sliderOffset.value, y, t: elapsed })
    }
}

async function endSlider() {
    if (!sliderDragging.value) return
    sliderDragging.value = false
    const lastTraceTime = sliderTrace.value.at(-1)?.t ?? -1
    const duration = Math.max(
        Date.now() - sliderStartedAt.value,
        lastTraceTime + 1,
    )
    recordSliderPoint(
        {
            clientX: sliderStartX.value + sliderOffset.value,
            clientY: 0,
            currentTarget: sliderTrack.value,
            pointerId: 0,
        },
        duration,
        true,
    )
    form.captcha = `${Math.round(sliderOffset.value)}~${duration}~${sliderTrace.value
        .map(
            (point) =>
                `${Math.round(point.x)},${Math.round(point.y)},${point.t}`,
        )
        .join(';')}`
    sliderModalOpen.value = false
    await performLogin()
}

async function refreshSlider() {
    await loadCaptcha()
    sliderModalOpen.value = true
}

function closeSlider() {
    sliderModalOpen.value = false
    sliderStatus.value = ''
    form.captcha = ''
}

function showSliderInfo() {
    message.info('请拖动圆形滑块，使拼图位置对齐')
}

async function submit() {
    if (
        captchaEnabled.value &&
        captchaType.value === 'slider' &&
        !form.captcha
    ) {
        sliderModalOpen.value = true
        return
    }
    await performLogin()
}

async function performLogin() {
    loading.value = true
    authStore.clearAuth()
    clearManagementRoutes()
    try {
        const login = await authApi.login(form)
        authStore.setToken(login.data.data.token, form.rememberMe)
        const [profile, routes] = await Promise.all([
            authApi.profile(),
            authApi.routes(),
        ])
        authStore.setSession(
            login.data.data.token,
            profile.data.data,
            routes.data.data,
            form.rememberMe,
        )
        ensureManagementRoutes(routes.data.data)
        const redirect =
            typeof route.query.redirect === 'string'
                ? route.query.redirect
                : '/'
        await router.replace(redirect)
    } catch (error: unknown) {
        authStore.clearAuth()
        clearManagementRoutes()
        const apiMessage = axios.isAxiosError(error)
            ? error.response?.data?.message
            : undefined
        message.error(
            typeof apiMessage === 'string' && apiMessage
                ? apiMessage
                : '登录失败，请检查账号和密码',
        )
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
                    v-if="captchaEnabled && captchaType === 'numeric'"
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
                    <div v-if="captchaType === 'numeric'" class="captcha-row">
                        <div v-if="captchaQuestion" class="captcha-question">
                            {{ captchaQuestion }}
                        </div>
                        <a-input
                            v-model:value="form.captcha"
                            :maxlength="16"
                            placeholder="请输入验证码"
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
                    ><a-checkbox
                        v-if="rememberMeEnabled"
                        v-model:checked="form.rememberMe"
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
            <a-modal
                v-model:open="sliderModalOpen"
                :title="null"
                :footer="null"
                centered
                :width="480"
                :closable="false"
                wrap-class-name="slider-captcha-modal"
                @cancel="closeSlider"
            >
                <div
                    class="slider-captcha"
                    aria-describedby="slider-captcha-status"
                    @pointermove="moveSlider"
                    @pointerup="endSlider"
                    @pointercancel="endSlider"
                >
                    <div
                        class="slider-captcha-image"
                        :style="{
                            width: `${sliderWidth}px`,
                            height: `${sliderHeight}px`,
                        }"
                    >
                        <img :src="sliderBackground" alt="滑块验证码背景" />
                        <img
                            class="slider-captcha-piece"
                            :src="sliderPiece"
                            alt="滑块拼图"
                            :style="{
                                width: `${sliderPieceWidth}px`,
                                height: `${sliderPieceWidth}px`,
                                top: `${sliderPieceTop}px`,
                                left: `${sliderOffset}px`,
                            }"
                        />
                    </div>
                    <div ref="sliderTrack" class="slider-captcha-track">
                        <div
                            class="slider-captcha-fill"
                            :style="{
                                width: `${sliderOffset + sliderPieceWidth / 2}px`,
                            }"
                        />
                        <span id="slider-captcha-status" aria-live="polite">
                            {{ sliderStatus || '拖动滑块完成验证' }}
                        </span>
                        <button
                            type="button"
                            class="slider-captcha-handle"
                            aria-label="拖动滑块"
                            :style="{ left: `${sliderOffset}px` }"
                            @pointerdown.stop="startSlider"
                        >
                            <HolderOutlined />
                        </button>
                    </div>
                    <div class="slider-captcha-toolbar">
                        <button
                            type="button"
                            title="关闭验证"
                            aria-label="关闭验证"
                            @click="closeSlider"
                        >
                            <CloseCircleOutlined />
                        </button>
                        <button
                            type="button"
                            title="刷新验证图片"
                            aria-label="刷新验证图片"
                            @click="refreshSlider"
                        >
                            <ReloadOutlined />
                        </button>
                        <button
                            type="button"
                            title="验证说明"
                            aria-label="验证说明"
                            @click="showSliderInfo"
                        >
                            <InfoCircleOutlined />
                        </button>
                        <span class="slider-captcha-brand">本地安全验证</span>
                    </div>
                </div>
            </a-modal>
        </section>
    </main>
</template>
