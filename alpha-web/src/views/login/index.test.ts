import Antd from 'ant-design-vue'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'

enableAutoUnmount(afterEach)

const { login, profile, routes, captcha, replace } = vi.hoisted(() => ({
    login: vi.fn(),
    profile: vi.fn(),
    routes: vi.fn(),
    captcha: vi.fn(),
    replace: vi.fn(),
}))

vi.mock('@/service/auth/index', () => ({
    authApi: { login, profile, routes, captcha },
}))
vi.mock('vue-router', () => ({
    useRoute: () => ({ query: {} }),
    useRouter: () => ({ replace }),
}))
vi.mock('@/router', () => ({
    clearManagementRoutes: vi.fn(),
    ensureManagementRoutes: vi.fn(),
}))

import Login from './index.vue'
import type { LoginPayload } from '@/service/auth/index'
import { authStore } from '@/stores/auth'

describe('login page', () => {
    it('blocks login until the captcha configuration finishes loading', async () => {
        let resolveCaptcha: ((value: unknown) => void) | undefined
        captcha.mockReturnValueOnce(
            new Promise((resolve) => {
                resolveCaptcha = resolve
            }),
        )
        login.mockClear()

        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('admin123')

        expect(
            wrapper.get('button[type="submit"]').attributes('disabled'),
        ).toBe('')
        await wrapper.get('form').trigger('submit')
        expect(login).not.toHaveBeenCalled()

        resolveCaptcha?.({
            data: {
                data: {
                    enabled: false,
                    type: 'numeric',
                    rememberMeEnabled: true,
                },
            },
        })
        await flushPromises()
        expect(
            wrapper.get('button[type="submit"]').attributes('disabled'),
        ).toBeUndefined()
    })

    it('stores the token before loading profile and routes', async () => {
        captcha.mockResolvedValue({
            data: {
                data: {
                    enabled: false,
                    type: 'numeric',
                    rememberMeEnabled: true,
                    captchaId: null,
                    image: null,
                    question: null,
                    sliderBackground: null,
                    sliderPiece: null,
                    sliderWidth: null,
                    sliderHeight: null,
                    sliderPieceWidth: null,
                    sliderPieceTop: null,
                },
            },
        })
        login.mockResolvedValue({
            data: {
                data: { token: 'token', tokenType: 'Bearer', expiresIn: 3600 },
            },
        })
        profile.mockImplementation(async () => {
            expect(authStore.getToken()).toBe('token')
            return {
                data: {
                    data: {
                        id: 1,
                        username: 'admin',
                        roles: [],
                        permissions: [],
                        mustChangePassword: false,
                    },
                },
            }
        })
        routes.mockResolvedValue({ data: { data: [] } })

        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('password')
        await wrapper.get('form').trigger('submit')
        await flushPromises()

        expect(authStore.getToken()).toBe('token')
        expect(authStore.state.profile?.username).toBe('admin')
        expect(replace).toHaveBeenCalledWith('/')
        authStore.clearAuth()
    })

    it('clears session data when login fails', async () => {
        authStore.setSession(
            'stale-token',
            {
                id: 1,
                username: 'admin',
                roles: [],
                permissions: [],
                mustChangePassword: false,
            },
            [],
        )
        captcha.mockResolvedValue({
            data: {
                data: {
                    enabled: false,
                    type: 'numeric',
                    rememberMeEnabled: true,
                    captchaId: null,
                    image: null,
                    question: null,
                    sliderBackground: null,
                    sliderPiece: null,
                    sliderWidth: null,
                    sliderHeight: null,
                    sliderPieceWidth: null,
                    sliderPieceTop: null,
                },
            },
        })
        login.mockRejectedValue(new Error('invalid credentials'))

        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('password')
        await wrapper.get('form').trigger('submit')
        await flushPromises()

        expect(authStore.getToken()).toBeNull()
        expect(authStore.state.profile).toBeNull()
        expect(authStore.state.routes).toEqual([])
    })

    it('renders the configured slider captcha and hides disabled remember-me option', async () => {
        captcha.mockResolvedValue({
            data: {
                data: {
                    enabled: true,
                    type: 'slider',
                    rememberMeEnabled: false,
                    captchaId: 'slider-id',
                    image: null,
                    question: '3 + 4 = ?',
                    sliderBackground: 'data:image/png;base64,background',
                    sliderPiece: 'data:image/png;base64,piece',
                    sliderWidth: 420,
                    sliderHeight: 280,
                    sliderPieceWidth: 42,
                    sliderPieceTop: 42,
                },
            },
        })
        const wrapper = mount(Login, { global: { plugins: [Antd] } })
        await flushPromises()
        await wrapper.find('input[autocomplete="username"]').setValue('admin')
        await wrapper
            .find('input[autocomplete="current-password"]')
            .setValue('password')
        await wrapper.get('form').trigger('submit')
        await flushPromises()
        expect(document.body.textContent).not.toContain(
            '请完成滑块验证后继续登录',
        )
        expect(document.body.textContent).toContain('拖动滑块完成验证')
        expect(document.body.querySelector('.slider-captcha')).not.toBeNull()
        expect(
            document.body.querySelector('.slider-captcha-toolbar'),
        ).not.toBeNull()
        expect(document.body.textContent).not.toContain('7 天内保持登录')
    })

    it('submits the final slider offset even when pointer-up follows the last move within 16ms', async () => {
        let now = 1_000
        const nowSpy = vi.spyOn(Date, 'now').mockImplementation(() => now)
        try {
            captcha.mockResolvedValue({
                data: {
                    data: {
                        enabled: true,
                        type: 'slider',
                        rememberMeEnabled: true,
                        captchaId: 'slider-id',
                        image: null,
                        question: null,
                        sliderBackground: 'data:image/png;base64,background',
                        sliderPiece: 'data:image/png;base64,piece',
                        sliderWidth: 420,
                        sliderHeight: 280,
                        sliderPieceWidth: 42,
                        sliderPieceTop: 42,
                    },
                },
            })
            login.mockClear()
            let submittedCaptcha = ''
            login.mockImplementationOnce(async (payload: LoginPayload) => {
                submittedCaptcha = payload.captcha ?? ''
                throw new Error('stop after payload capture')
            })

            const wrapper = mount(Login, { global: { plugins: [Antd] } })
            await flushPromises()
            await wrapper
                .find('input[autocomplete="username"]')
                .setValue('admin')
            await wrapper
                .find('input[autocomplete="current-password"]')
                .setValue('admin123')
            await wrapper.get('form').trigger('submit')
            await flushPromises()
            expect(login).not.toHaveBeenCalled()

            const latest = (selector: string) =>
                Array.from(
                    document.body.querySelectorAll<HTMLElement>(selector),
                ).at(-1) as HTMLElement
            const track = latest('.slider-captcha-track')
            const handle = latest('.slider-captcha-handle') as HTMLElement & {
                setPointerCapture: (pointerId: number) => void
            }
            const captchaPanel = latest('.slider-captcha')
            track.getBoundingClientRect = () =>
                ({ left: 0, top: 0, width: 350 }) as DOMRect
            handle.getBoundingClientRect = () => ({ width: 50 }) as DOMRect
            handle.setPointerCapture = vi.fn()
            const dispatchPointerEvent = (
                target: HTMLElement,
                type: string,
                clientX: number,
            ) => {
                const event = new MouseEvent(type, {
                    bubbles: true,
                    clientX,
                    clientY: 24,
                })
                Object.defineProperty(event, 'pointerId', { value: 1 })
                target.dispatchEvent(event)
            }

            dispatchPointerEvent(handle, 'pointerdown', 6)
            for (const [elapsed, clientX] of [
                [400, 75],
                [500, 145],
                [600, 215],
                [615, 296],
            ] as const) {
                now = 1_000 + elapsed
                dispatchPointerEvent(captchaPanel, 'pointermove', clientX)
            }
            now = 1_630
            dispatchPointerEvent(captchaPanel, 'pointerup', 296)
            await flushPromises()

            expect(login).toHaveBeenCalledOnce()
            expect(submittedCaptcha).toContain('~')
            const [submittedX, duration, trace] = submittedCaptcha.split('~')
            const lastPoint = trace.split(';').at(-1)?.split(',')
            expect(submittedX).toBe('365')
            expect(duration).toBe('630')
            expect(lastPoint?.[0]).toBe(submittedX)
            expect(lastPoint?.[2]).toBe(duration)
        } finally {
            nowSpy.mockRestore()
        }
    })
})
