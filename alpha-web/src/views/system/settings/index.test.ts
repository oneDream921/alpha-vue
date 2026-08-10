import Antd from 'ant-design-vue'
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

const {
    get,
    save,
    regenerateRsaKeys,
    publishOfficialAccountMenu,
    revealFileStorageCredentials,
} = vi.hoisted(() => ({
    get: vi.fn(),
    save: vi.fn(),
    regenerateRsaKeys: vi.fn(),
    publishOfficialAccountMenu: vi.fn(),
    revealFileStorageCredentials: vi.fn(),
}))

vi.mock('@/service/system/settings', () => ({
    systemSettingApi: {
        get,
        save,
        regenerateRsaKeys,
        publishOfficialAccountMenu,
        revealFileStorageCredentials,
        createSimulationOrder: vi.fn(),
        completeSimulationOrder: vi.fn(),
    },
}))
vi.mock('@/service/files', () => ({
    fileApi: {
        upload: vi.fn(),
        accessUrl: vi.fn(),
        content: vi.fn().mockRejectedValue(new Error()),
    },
}))

import Settings from './index.vue'

const minioSetting = {
    group: 'file',
    values: {
        provider: 'minio',
        endpoint: 'http://localhost:19000',
        bucket: 'alpha-vue',
        maxSizeMb: 10,
        allowedExtensions: 'png,jpg,pdf',
        publicAccess: false,
    },
    secretConfigured: { accessKey: true, secretKey: true },
    restartRequired: false,
}

function response(group: string) {
    return {
        data: {
            data:
                group === 'file'
                    ? minioSetting
                    : {
                          group,
                          values: {},
                          secretConfigured: {},
                          restartRequired: false,
                      },
        },
    }
}

describe('system settings page', () => {
    it('loads every settings tab through its matching group', async () => {
        get.mockImplementation((group: string) =>
            Promise.resolve(response(group)),
        )
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()

        for (const name of [
            '站点配置',
            '登录配置',
            '文件配置',
            '第三方登录',
            '支付配置',
            '安全配置',
            '小程序',
            '公众号',
        ]) {
            await wrapper
                .findAll('[role="tab"]')
                .find((tab) => tab.text() === name)!
                .trigger('click')
            await flushPromises()
        }

        expect(get.mock.calls.map(([group]) => group)).toEqual(
            expect.arrayContaining([
                'site',
                'login',
                'file',
                'oauth',
                'payment',
                'security',
                'mini_program',
                'official_account',
            ]),
        )
    }, 15_000)

    it('renders only the active MinIO fields and keeps configured secrets masked', async () => {
        get.mockImplementation((group: string) =>
            Promise.resolve(response(group)),
        )
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()
        await wrapper
            .findAll('[role="tab"]')
            .find((tab) => tab.text() === '文件配置')!
            .trigger('click')
        await flushPromises()

        expect(wrapper.text()).toContain('MinIO Endpoint')
        expect(wrapper.text()).toContain('Bucket 名称')
        expect(wrapper.text()).not.toContain('本地存储路径')
        expect(wrapper.text()).toContain('已配置，留空不修改')
        expect(wrapper.text()).toContain('png')
    })

    it('replaces an unavailable logo preview with a recovery hint', async () => {
        get.mockResolvedValue({
            data: {
                data: {
                    group: 'site',
                    values: { siteLogo: '/expired-logo.png' },
                    secretConfigured: {},
                    restartRequired: false,
                },
            },
        })
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()

        await wrapper.get('img[alt="站点 Logo 预览"]').trigger('error')
        await flushPromises()

        expect(wrapper.find('img[alt="站点 Logo 预览"]').exists()).toBe(false)
        expect(wrapper.text()).toContain(
            '当前 Logo 无法加载，请重新上传图片后保存',
        )
    })

    it('reveals file storage credentials through independent eye controls', async () => {
        get.mockImplementation((group: string) =>
            Promise.resolve(response(group)),
        )
        revealFileStorageCredentials.mockResolvedValue({
            data: {
                data: { accessKey: 'alpha-access', secretKey: 'alpha-secret' },
            },
        })
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()
        await wrapper
            .findAll('[role="tab"]')
            .find((tab) => tab.text() === '文件配置')!
            .trigger('click')
        await flushPromises()

        expect(revealFileStorageCredentials).not.toHaveBeenCalled()
        expect(
            wrapper.find('button[aria-label="显示 Access Key"]').exists(),
        ).toBe(true)
        expect(
            wrapper.find('button[aria-label="显示 Secret Key"]').exists(),
        ).toBe(true)
        await wrapper
            .get('button[aria-label="显示 Access Key"]')
            .trigger('click')
        await flushPromises()

        expect(revealFileStorageCredentials).toHaveBeenCalledOnce()
        expect(
            wrapper.find('button[aria-label="隐藏 Access Key"]').exists(),
        ).toBe(true)
        expect(
            wrapper.find('button[aria-label="显示 Secret Key"]').exists(),
        ).toBe(true)
    })

    it('offers custom and username watermark modes', async () => {
        get.mockResolvedValue({
            data: {
                data: {
                    group: 'site',
                    values: {
                        watermarkEnabled: true,
                        watermarkType: 'username',
                        watermarkContent: '',
                    },
                    secretConfigured: {},
                    restartRequired: false,
                },
            },
        })
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()

        expect(wrapper.text()).toContain('水印类型')
        expect(wrapper.find('.ant-select').exists()).toBe(true)
        expect(wrapper.text()).toContain('使用用户名')
        expect(
            wrapper.find('input[placeholder="将使用当前登录用户名"]').exists(),
        ).toBe(true)
    })

    it('renders captcha type as a selector', async () => {
        get.mockResolvedValue({
            data: {
                data: {
                    group: 'login',
                    values: {
                        captchaEnabled: true,
                        captchaType: 'slider',
                        rememberMeEnabled: true,
                    },
                    secretConfigured: {},
                    restartRequired: false,
                },
            },
        })
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()
        await wrapper
            .findAll('[role="tab"]')
            .find((tab) => tab.text() === '登录配置')!
            .trigger('click')
        await flushPromises()
        expect(wrapper.find('.ant-select').exists()).toBe(true)
        await wrapper.find('.ant-select').trigger('click')
        await flushPromises()
        expect(wrapper.text()).toContain('滑块验证')
    })

    it('reveals dependent settings only after their feature is enabled', async () => {
        get.mockResolvedValue({
            data: {
                data: {
                    group: 'site',
                    values: { watermarkEnabled: false },
                    secretConfigured: {},
                    restartRequired: false,
                },
            },
        })
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()

        expect(wrapper.text()).not.toContain('水印类型')
        await wrapper.get('[role="switch"]').trigger('click')
        await flushPromises()
        expect(wrapper.text()).toContain('水印类型')
        expect(wrapper.text()).toContain('有未保存的修改')
    })

    it('shows a recoverable page error when the active group cannot load', async () => {
        get.mockRejectedValue(new Error('offline'))
        const wrapper = mount(Settings, {
            global: {
                plugins: [Antd],
                directives: { permission: () => undefined },
            },
        })
        await flushPromises()

        expect(wrapper.text()).toContain('配置加载失败')
        expect(wrapper.text()).toContain('重试')
        expect(wrapper.find('form').exists()).toBe(false)
    })
})
