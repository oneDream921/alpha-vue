<script setup lang="ts">
import { UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { authApi } from '@/service/auth'
import { authStore } from '@/stores/auth'
import AvatarCropper from './AvatarCropper.vue'

const router = useRouter()
const avatarUploading = ref(false)
const avatarCropperOpen = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()
const profile = authStore.state.profile
const form = reactive({
    nickname: profile?.nickname ?? '',
    avatar: profile?.avatar ?? '',
    email: profile?.email ?? '',
    phone: profile?.phone ?? '',
})
const password = reactive({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
})
const profileRules: Record<string, Rule[]> = {
    nickname: [
        { required: true, whitespace: true, message: '请输入昵称' },
        { max: 64, message: '昵称不能超过 64 个字符' },
    ],
    email: [{ type: 'email', message: '请输入正确的邮箱地址' }],
    phone: [
        { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入 11 位中国大陆手机号' },
    ],
    avatar: [{ max: 255, message: '头像地址不能超过 255 个字符' }],
}
const passwordRules: Record<string, Rule[]> = {
    currentPassword: [{ required: true, message: '请输入当前密码' }],
    newPassword: [
        { required: true, message: '请输入新密码' },
        { min: 8, max: 100, message: '新密码长度为 8 至 100 个字符' },
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码' },
        {
            validator: async (_rule: unknown, value: string) => {
                if (value !== password.newPassword)
                    throw new Error('两次输入的新密码不一致')
            },
        },
    ],
}

async function saveProfile() {
    const response = await authApi.updateProfile({
        nickname: form.nickname,
        avatar: form.avatar || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
    })
    authStore.setProfile(response.data.data)
    message.success('资料已更新')
}
async function submitProfile() {
    try {
        await profileFormRef.value?.validate()
        await saveProfile()
    } catch {
        // 表单校验提示或全局请求提示已经向用户展示，不再抛出未处理的异步异常。
    }
}
async function uploadAvatar(file: File) {
    avatarUploading.value = true
    try {
        const response = await authApi.uploadAvatar(file)
        form.avatar = response.data.data.avatar ?? ''
        authStore.setProfile(response.data.data)
        message.success('头像已更新')
    } catch {
        message.error('头像上传失败，请检查图片类型和大小')
    } finally {
        avatarUploading.value = false
    }
    return false
}
async function changePassword() {
    await authApi.changePassword({
        currentPassword: password.currentPassword,
        newPassword: password.newPassword,
    })
    authStore.clearAuth()
    message.success('密码已更新，请重新登录')
    await router.replace('/login')
}
async function submitPassword() {
    try {
        await passwordFormRef.value?.validate()
        await changePassword()
    } catch {
        // 表单校验提示或全局请求提示已经向用户展示，不再抛出未处理的异步异常。
    }
}
</script>

<template>
    <section class="page-section">
        <div class="page-heading">
            <div>
                <h1>个人中心</h1>
                <p>维护当前账号资料和登录密码</p>
            </div>
        </div>
        <a-tabs>
            <a-tab-pane key="profile" tab="基本资料"
                ><a-form
                    ref="profileFormRef"
                    :model="form"
                    :rules="profileRules"
                    layout="vertical"
                    class="profile-form"
                    ><div class="avatar-editor">
                        <a-avatar
                            :size="72"
                            :src="form.avatar"
                            class="profile-avatar"
                        >
                            <template #icon><UserOutlined /></template>
                        </a-avatar>
                        <div class="avatar-editor-actions">
                            <strong>个人头像</strong>
                            <span
                                >支持
                                PNG、JPG、GIF、WebP，大小限制以服务端配置为准</span
                            >
                            <a-button
                                :loading="avatarUploading"
                                @click="avatarCropperOpen = true"
                                >裁剪并上传</a-button
                            >
                        </div>
                    </div>
                    <div class="form-grid">
                        <a-form-item label="账号"
                            ><a-input
                                :value="profile?.username"
                                disabled /></a-form-item
                        ><a-form-item label="昵称" name="nickname" required
                            ><a-input v-model:value="form.nickname"
                        /></a-form-item>
                    </div>
                    <div class="form-grid">
                        <a-form-item label="邮箱" name="email"
                            ><a-input v-model:value="form.email" /></a-form-item
                        ><a-form-item label="手机" name="phone"
                            ><a-input v-model:value="form.phone"
                        /></a-form-item>
                    </div>
                    <a-form-item label="头像地址" name="avatar"
                        ><a-input v-model:value="form.avatar" /></a-form-item
                    ><a-form-item
                        ><a-button type="primary" @click="submitProfile"
                            >保存资料</a-button
                        ></a-form-item
                    ></a-form
                ></a-tab-pane
            >
            <a-tab-pane key="password" tab="修改密码"
                ><a-form
                    ref="passwordFormRef"
                    :model="password"
                    :rules="passwordRules"
                    layout="vertical"
                    class="password-form"
                    ><a-form-item
                        label="当前密码"
                        name="currentPassword"
                        required
                        ><a-input-password
                            v-model:value="password.currentPassword"
                            autocomplete="current-password" /></a-form-item
                    ><a-form-item label="新密码" name="newPassword" required
                        ><a-input-password
                            v-model:value="password.newPassword"
                            autocomplete="new-password" /></a-form-item
                    ><a-form-item
                        label="确认新密码"
                        name="confirmPassword"
                        required
                        ><a-input-password
                            v-model:value="password.confirmPassword"
                            autocomplete="new-password" /></a-form-item
                    ><a-button type="primary" @click="submitPassword"
                        >更新密码</a-button
                    ></a-form
                ></a-tab-pane
            >
        </a-tabs>
        <AvatarCropper
            v-model:open="avatarCropperOpen"
            @confirm="uploadAvatar"
        />
    </section>
</template>
