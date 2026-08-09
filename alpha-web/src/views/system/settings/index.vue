<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { EyeInvisibleOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { fileApi } from '@/service/files'
import {
    systemSettingApi,
    type SettingGroup,
    type SystemSetting,
} from '@/service/system/settings'

type Tab = { key: SettingGroup; label: string }
type FileCredentialKey = 'accessKey' | 'secretKey'

const tabs: Tab[] = [
    { key: 'site', label: '站点配置' },
    { key: 'login', label: '登录配置' },
    { key: 'file', label: '文件配置' },
    { key: 'oauth', label: '第三方登录' },
    { key: 'payment', label: '支付配置' },
    { key: 'security', label: '安全配置' },
    { key: 'mini_program', label: '小程序' },
    { key: 'official_account', label: '公众号' },
]
const activeKey = ref<SettingGroup>('site')
const isLoading = ref(false)
const isSaving = ref(false)
const setting = ref<SystemSetting | null>(null)
const form = reactive<Record<string, unknown>>({})
const allowedExtensionTags = ref<string[]>([])
const isUploadingLogo = ref(false)
const isLogoPreviewUnavailable = ref(false)
const logoPreviewUrl = ref<string | null>(null)
let logoPreviewObjectUrl: string | null = null
const isRevealingFileCredential = ref(false)
const fileCredentialVisibility = reactive<Record<FileCredentialKey, boolean>>({
    accessKey: false,
    secretKey: false,
})
const simulationOrder = ref<{
    id: number
    orderNo: string
    status: string
} | null>(null)

const fields = computed(() => {
    const group = activeKey.value
    const fileProvider = String(form.provider ?? 'local')
    const fileField = (key: string, label: string): [string, string] => [
        key,
        label,
    ]
    const fileFields: Array<[string, string]> = [
        fileField('provider', '存储方式'),
        ...(fileProvider === 'minio'
            ? [
                  fileField('endpoint', 'MinIO Endpoint'),
                  fileField('bucket', 'Bucket 名称'),
              ]
            : []),
        ...(fileProvider === 'oss'
            ? [
                  fileField('endpoint', 'OSS Endpoint'),
                  fileField('bucket', 'Bucket 名称'),
              ]
            : []),
        ...(fileProvider === 'cos'
            ? [fileField('region', '地域'), fileField('bucket', 'Bucket 名称')]
            : []),
        fileField('maxSizeMb', '最大文件大小（MB）'),
        fileField('allowedExtensions', '允许的文件类型（可多选）'),
        fileField('publicAccess', '公开访问'),
        ...(form.publicAccess === true
            ? [fileField('accessDomain', '访问域名')]
            : []),
        ...(fileProvider === 'local'
            ? [fileField('storagePath', '本地存储路径')]
            : []),
        ...(fileProvider !== 'local'
            ? [
                  fileField('accessKey', 'Access Key'),
                  fileField('secretKey', 'Secret Key'),
              ]
            : []),
    ]
    return (
        {
            site: [
                ['siteName', '站点名称'],
                ['siteDescription', '站点描述'],
                ['siteLogo', '站点 Logo'],
                ['copyright', '版权信息'],
                ['icp', 'ICP备案号'],
                ['watermarkEnabled', '启用水印'],
                ['watermarkType', '水印类型'],
                ['watermarkContent', '水印内容'],
                ['watermarkOpacity', '水印透明度'],
            ],
            login: [
                ['captchaEnabled', '验证码开关'],
                ['captchaType', '验证码类型'],
                ['maxRetry', '最大重试次数'],
                ['lockMinutes', '锁定时间（分钟）'],
                ['rememberMeEnabled', '记住我'],
            ],
            file: fileFields,
            oauth: [
                ['wechatEnabled', '微信登录'],
                ['wechatAppId', '微信 AppID'],
                ['wechatAppSecret', '微信 AppSecret'],
                ['alipayEnabled', '支付宝登录'],
                ['alipayAppId', '支付宝 AppID'],
                ['alipayAppSecret', '支付宝应用私钥（PKCS#8）'],
                ['githubEnabled', 'GitHub 登录'],
                ['githubClientId', 'GitHub Client ID'],
                ['githubClientSecret', 'GitHub Client Secret'],
                ['callbackBaseUrl', '回调基础地址'],
            ],
            payment: [
                ['wechatEnabled', '微信支付'],
                ['wechatMerchantId', '微信商户号'],
                ['wechatApiV3Key', '微信 API v3 密钥'],
                ['wechatPrivateKey', '微信私钥'],
                ['wechatNotifyUrl', '微信回调 URL'],
                ['alipayEnabled', '支付宝支付'],
                ['alipayAppId', '支付宝 AppID'],
                ['alipayPrivateKey', '支付宝私钥'],
                ['alipayPublicKey', '支付宝公钥'],
                ['alipayNotifyUrl', '支付宝回调 URL'],
            ],
            security: [
                ['xssFilteringEnabled', '开启 XSS 过滤'],
                ['rsaPublicKey', 'RSA 公钥'],
                ['rsaPrivateKey', 'RSA 私钥'],
            ],
            mini_program: [
                ['appId', 'AppID'],
                ['appSecret', 'AppSecret'],
            ],
            official_account: [
                ['appId', 'AppID'],
                ['appSecret', 'AppSecret'],
                ['token', 'Token'],
                ['encodingAesKey', 'EncodingAESKey'],
                ['callbackUrl', '回调 URL'],
                ['oauthCallbackUrl', 'OAuth 回调 URL'],
                ['customMenuJson', '自定义菜单 JSON'],
            ],
        } satisfies Record<SettingGroup, Array<[string, string]>>
    )[group]
})

function isSensitive(key: string) {
    return (
        Boolean(setting.value?.secretConfigured?.[key]) ||
        /secret|private|token|accessKey|apiV3Key|encodingAesKey/i.test(key)
    )
}

function shouldMaskSensitiveField(key: string) {
    return (
        isSensitive(key) &&
        !(isFileCredentialKey(key) && fileCredentialVisibility[key])
    )
}

function isFileCredentialKey(key: string): key is FileCredentialKey {
    return key === 'accessKey' || key === 'secretKey'
}

function resetFileCredentialVisibility() {
    form.accessKey = ''
    form.secretKey = ''
    fileCredentialVisibility.accessKey = false
    fileCredentialVisibility.secretKey = false
}

function fillMinioDefaults() {
    if (form.provider !== 'minio') return
    form.endpoint ||= 'http://localhost:19000'
    form.bucket ||= 'alpha-vue'
    form.region ||= 'us-east-1'
    form.maxSizeMb ||= 10
    const defaults = 'txt,pdf,png,jpg,jpeg,gif,webp,doc,docx,xls,xlsx'
    form.allowedExtensions ||= defaults
    if (allowedExtensionTags.value.length === 0) {
        allowedExtensionTags.value = defaults.split(',')
    }
    form.publicAccess ??= false
}

async function load() {
    isLoading.value = true
    try {
        resetFileCredentialVisibility()
        isLogoPreviewUnavailable.value = false
        const response = await systemSettingApi.get(activeKey.value)
        setting.value = response.data.data
        Object.keys(form).forEach((key) => delete form[key])
        Object.assign(form, response.data.data.values)
        if (activeKey.value === 'login') {
            form.captchaType =
                form.captchaType === 'slider' ? 'slider' : 'numeric'
        }
        const existingLogoId = logoFileId(String(form.siteLogo ?? ''))
        if (existingLogoId) form.siteLogo = `file:${existingLogoId}`
        if (activeKey.value === 'site') form.watermarkType ??= 'custom'
        await refreshLogoPreview()
        if (activeKey.value === 'file') {
            allowedExtensionTags.value = String(
                response.data.data.values.allowedExtensions ?? '',
            )
                .split(',')
                .map((extension) => extension.trim())
                .filter(Boolean)
            fillMinioDefaults()
        }
    } finally {
        isLoading.value = false
    }
}

async function save() {
    isSaving.value = true
    try {
        const hasLocalLogoPreview = logoPreviewObjectUrl !== null
        const values = { ...form }
        if (activeKey.value === 'file') {
            values.allowedExtensions = allowedExtensionTags.value.join(',')
        }
        const response = await systemSettingApi.save(activeKey.value, values)
        setting.value = response.data.data
        resetFileCredentialVisibility()
        message.success(
            response.data.data.restartRequired
                ? '已保存，部分配置将在重启后生效'
                : '系统配置已保存',
        )
        Object.keys(form).forEach((key) => delete form[key])
        Object.assign(form, response.data.data.values)
        if (!hasLocalLogoPreview) await refreshLogoPreview()
        if (activeKey.value === 'file') {
            allowedExtensionTags.value = String(
                response.data.data.values.allowedExtensions ?? '',
            )
                .split(',')
                .map((extension) => extension.trim())
                .filter(Boolean)
        }
    } finally {
        isSaving.value = false
    }
}

async function toggleFileCredential(key: FileCredentialKey) {
    if (fileCredentialVisibility[key]) {
        form[key] = ''
        fileCredentialVisibility[key] = false
        return
    }
    isRevealingFileCredential.value = true
    try {
        const response = await systemSettingApi.revealFileStorageCredentials()
        form[key] = response.data.data[key] ?? ''
        fileCredentialVisibility[key] = true
    } finally {
        isRevealingFileCredential.value = false
    }
}

async function uploadLogo(file: File) {
    isUploadingLogo.value = true
    try {
        const response = await fileApi.upload(file)
        form.siteLogo = `file:${response.data.data.id}`
        replaceLogoPreviewUrl(createLogoObjectUrl(file))
        isLogoPreviewUnavailable.value = false
        message.success('Logo 上传成功，请点击保存')
    } finally {
        isUploadingLogo.value = false
    }
    return false
}

function replaceLogoPreviewUrl(url: string | null) {
    if (logoPreviewObjectUrl) URL.revokeObjectURL(logoPreviewObjectUrl)
    logoPreviewObjectUrl = url?.startsWith('blob:') ? url : null
    logoPreviewUrl.value = url
}

function createLogoObjectUrl(value: Blob) {
    return typeof URL.createObjectURL === 'function'
        ? URL.createObjectURL(value)
        : null
}

function logoFileId(value: string) {
    const match = value.match(/(?:file:|\/files\/)(\d+)/)
    return match ? Number(match[1]) : null
}

async function refreshLogoPreview() {
    const value = String(form.siteLogo ?? '').trim()
    isLogoPreviewUnavailable.value = false
    if (!value) {
        replaceLogoPreviewUrl(null)
        return
    }
    try {
        const id = logoFileId(value)
        const accessUrl = id ? (await fileApi.accessUrl(id)).data.data : value
        const response = await fileApi.content(accessUrl, 'arraybuffer', {
            silent: true,
        })
        const contentType = response.headers['content-type']
        replaceLogoPreviewUrl(
            createLogoObjectUrl(
                new Blob([response.data], {
                    type:
                        typeof contentType === 'string'
                            ? contentType
                            : undefined,
                }),
            ) ?? value,
        )
    } catch {
        replaceLogoPreviewUrl(value)
    }
}

function handleLogoPreviewError() {
    isLogoPreviewUnavailable.value = true
}

async function createSimulationOrder(channel: 'wechat' | 'alipay') {
    const response = await systemSettingApi.createSimulationOrder(
        channel,
        100,
        globalThis.crypto.randomUUID(),
    )
    simulationOrder.value = response.data.data
    message.success('模拟订单已创建')
}

async function completeSimulationOrder(status: 'succeeded' | 'failed') {
    if (!simulationOrder.value) return
    const response = await systemSettingApi.completeSimulationOrder(
        simulationOrder.value.id,
        status,
    )
    simulationOrder.value = response.data.data
    message.success(status === 'succeeded' ? '模拟支付成功' : '模拟支付失败')
}

async function regenerateRsaKeys() {
    isSaving.value = true
    try {
        const response = await systemSettingApi.regenerateRsaKeys()
        form.rsaPublicKey = response.data.data.publicKey
        form.rsaPrivateKey = response.data.data.privateKey
        message.success('RSA 密钥已回填到表单，请点击“保存”写入数据库')
    } finally {
        isSaving.value = false
    }
}

async function publishOfficialAccountMenu() {
    isSaving.value = true
    try {
        await systemSettingApi.publishOfficialAccountMenu()
        message.success('公众号菜单已发布')
    } finally {
        isSaving.value = false
    }
}

onMounted(() => void load())
watch(activeKey, () => void load())
onBeforeUnmount(() => replaceLogoPreviewUrl(null))
</script>

<template>
    <a-card title="系统配置" :bordered="false">
        <a-tabs v-model:active-key="activeKey">
            <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
        </a-tabs>
        <a-spin :spinning="isLoading">
            <a-form layout="vertical" class="settings-form">
                <a-form-item
                    v-for="[key, label] in fields"
                    :key="key"
                    :label="label"
                >
                    <template
                        v-if="
                            typeof form[key] === 'boolean' ||
                            /Enabled$|Access$|Filtering/.test(key)
                        "
                    >
                        <a-switch v-model:checked="form[key] as boolean" />
                    </template>
                    <template v-else-if="key === 'provider'">
                        <a-select
                            v-model:value="form[key] as string"
                            :options="[
                                { value: 'local', label: '本地存储' },
                                { value: 'minio', label: 'MinIO' },
                                { value: 'oss', label: '阿里云 OSS' },
                                { value: 'cos', label: '腾讯云 COS' },
                            ]"
                            @change="fillMinioDefaults"
                        />
                    </template>
                    <template v-else-if="key === 'captchaType'">
                        <a-select
                            v-model:value="form[key] as string"
                            :options="[
                                { value: 'numeric', label: '数字验证码' },
                                { value: 'slider', label: '滑块验证' },
                            ]"
                        />
                    </template>
                    <template v-else-if="key === 'siteLogo'">
                        <a-space
                            direction="vertical"
                            align="start"
                            size="small"
                            class="logo-setting"
                        >
                            <a-upload
                                accept="image/png,image/jpeg,image/webp,image/gif"
                                :show-upload-list="false"
                                :before-upload="uploadLogo"
                            >
                                <a-button :loading="isUploadingLogo">
                                    {{ form[key] ? '更换图片' : '上传图片' }}
                                </a-button>
                            </a-upload>
                            <a-typography-text type="secondary">
                                支持 PNG、JPG、GIF、WebP 格式
                            </a-typography-text>
                            <img
                                v-if="
                                    form[key] &&
                                    logoPreviewUrl &&
                                    !isLogoPreviewUnavailable
                                "
                                :src="logoPreviewUrl"
                                alt="站点 Logo 预览"
                                class="logo-preview"
                                @error="handleLogoPreviewError"
                            />
                            <a-alert
                                v-else-if="form[key]"
                                type="warning"
                                show-icon
                                message="当前 Logo 无法加载，请重新上传图片后保存"
                            />
                            <a-typography-text v-else type="secondary">
                                尚未设置站点 Logo
                            </a-typography-text>
                        </a-space>
                    </template>
                    <a-select
                        v-else-if="key === 'watermarkType'"
                        v-model:value="form[key] as string"
                        :options="[
                            { value: 'custom', label: '自定义内容' },
                            { value: 'username', label: '使用用户名' },
                        ]"
                        class="field-input"
                    />
                    <template v-else-if="key === 'allowedExtensions'">
                        <a-select
                            v-model:value="allowedExtensionTags"
                            mode="tags"
                            :token-separators="[',', ' ']"
                            placeholder="输入或粘贴扩展名，例如 png,jpg,pdf"
                            class="field-input"
                        />
                    </template>
                    <a-input-number
                        v-else-if="/Retry|Minutes|Mb/.test(key)"
                        v-model:value="form[key] as number"
                        :min="0"
                        class="field-input"
                    />
                    <a-slider
                        v-else-if="key === 'watermarkOpacity'"
                        v-model:value="form[key] as number"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        class="field-input"
                    />
                    <a-input
                        v-else-if="isFileCredentialKey(key)"
                        v-model:value="form[key] as string"
                        :type="
                            shouldMaskSensitiveField(key) ? 'password' : 'text'
                        "
                        :placeholder="
                            shouldMaskSensitiveField(key)
                                ? '留空则保持原值'
                                : undefined
                        "
                        class="field-input"
                    >
                        <template #suffix>
                            <a-button
                                v-permission="'system:setting:update'"
                                type="text"
                                size="small"
                                :loading="isRevealingFileCredential"
                                :aria-label="
                                    fileCredentialVisibility[key]
                                        ? `隐藏 ${label}`
                                        : `显示 ${label}`
                                "
                                :title="
                                    fileCredentialVisibility[key]
                                        ? `隐藏 ${label}`
                                        : `显示 ${label}`
                                "
                                @click.stop="toggleFileCredential(key)"
                            >
                                <template #icon>
                                    <EyeInvisibleOutlined
                                        v-if="fileCredentialVisibility[key]"
                                    />
                                    <EyeOutlined v-else />
                                </template>
                            </a-button>
                        </template>
                    </a-input>
                    <a-textarea
                        v-else-if="/Key$/.test(key)"
                        v-model:value="form[key] as string"
                        :placeholder="
                            shouldMaskSensitiveField(key)
                                ? '留空则保持原值'
                                : undefined
                        "
                        :auto-size="{ minRows: 2, maxRows: 5 }"
                    />
                    <a-input
                        v-else-if="key === 'watermarkContent'"
                        v-model:value="form[key] as string"
                        :disabled="form.watermarkType === 'username'"
                        :placeholder="
                            form.watermarkType === 'username'
                                ? '将使用当前登录用户名'
                                : '请输入水印内容'
                        "
                        class="field-input"
                    />
                    <a-input
                        v-else
                        v-model:value="form[key] as string"
                        :type="
                            shouldMaskSensitiveField(key) ? 'password' : 'text'
                        "
                        :placeholder="
                            shouldMaskSensitiveField(key)
                                ? '留空则保持原值'
                                : undefined
                        "
                        class="field-input"
                    />
                    <span v-if="key === 'watermarkOpacity'" class="field-hint">
                        {{ Math.round(Number(form[key] ?? 0) * 100) }}%
                    </span>
                    <div v-if="key === 'allowedExtensions'" class="field-hint">
                        可逐个输入扩展名并按 Enter 添加，也可直接粘贴
                        png,jpg,pdf；保存时统一按逗号分隔。
                    </div>
                    <div v-else-if="isSensitive(key)" class="secret-hint">
                        {{
                            setting?.secretConfigured?.[key]
                                ? '已配置，留空不修改'
                                : '首次配置时填写；保存后不会回显'
                        }}
                        <template v-if="isFileCredentialKey(key)">
                            ；点击输入框右侧眼睛查看，保存或切换页签后自动隐藏
                        </template>
                    </div>
                    <div v-if="key === 'watermarkType'" class="field-hint">
                        使用用户名时，水印会显示当前登录用户的昵称或用户名。
                    </div>
                </a-form-item>
                <a-space wrap>
                    <a-button
                        v-permission="'system:setting:update'"
                        type="primary"
                        :loading="isSaving"
                        @click="save"
                        >保存</a-button
                    >
                </a-space>
                <template v-if="activeKey === 'payment'">
                    <a-divider />
                    <a-space wrap>
                        <a-button @click="createSimulationOrder('wechat')"
                            >创建微信模拟订单</a-button
                        >
                        <a-button @click="createSimulationOrder('alipay')"
                            >创建支付宝模拟订单</a-button
                        >
                        <a-button
                            :disabled="
                                !simulationOrder ||
                                simulationOrder.status !== 'PENDING'
                            "
                            @click="completeSimulationOrder('succeeded')"
                            >模拟成功</a-button
                        >
                        <a-button
                            danger
                            :disabled="
                                !simulationOrder ||
                                simulationOrder.status !== 'PENDING'
                            "
                            @click="completeSimulationOrder('failed')"
                            >模拟失败</a-button
                        >
                    </a-space>
                    <a-typography-text
                        v-if="simulationOrder"
                        class="simulation-order"
                        >{{ simulationOrder.orderNo }}：{{
                            simulationOrder.status
                        }}</a-typography-text
                    >
                </template>
                <template v-if="activeKey === 'security'">
                    <a-divider />
                    <a-button
                        v-permission="'system:setting:update'"
                        :loading="isSaving"
                        @click="regenerateRsaKeys"
                        >重新生成 RSA 密钥</a-button
                    >
                </template>
                <template v-if="activeKey === 'official_account'">
                    <a-divider />
                    <a-button
                        v-permission="'system:setting:update'"
                        :loading="isSaving"
                        @click="publishOfficialAccountMenu"
                        >发布自定义菜单</a-button
                    >
                </template>
            </a-form>
        </a-spin>
    </a-card>
</template>

<style scoped>
.settings-form {
    max-width: 760px;
}
.field-input {
    width: min(100%, 560px);
}
.secret-hint {
    color: var(--alpha-text-secondary);
    font-size: 12px;
    margin-top: 4px;
}
.field-hint {
    color: var(--alpha-text-secondary);
    font-size: 12px;
    margin-top: 4px;
}
.simulation-order {
    display: block;
    margin-top: 12px;
}
.logo-setting :deep(.ant-typography) {
    font-size: 12px;
}
.logo-preview {
    display: block;
    width: 160px;
    height: 64px;
    object-fit: contain;
    border: 1px solid var(--alpha-border);
    border-radius: var(--alpha-radius);
    background: var(--alpha-surface);
}
</style>
