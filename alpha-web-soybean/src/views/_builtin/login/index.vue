<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import CryptoJS from 'crypto-js';
import { LockOutlined, ReloadOutlined, UserOutlined } from '@ant-design/icons-vue';
import { fetchGetBackendRoutes } from '@/service/api/route';
import {
  fetchCheckSliderCaptcha,
  fetchGetCaptcha,
  fetchGetSliderCaptcha,
  fetchGetUserInfo,
  fetchLogin
} from '@/service/api/auth';
import logoUrl from '@/assets/alpha-logo.svg';
import { authStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const captchaLoading = ref(true);
const captchaLoadFailed = ref(false);
const loginFormRef = ref();
const form = reactive({
  username: '',
  password: '',
  deviceId: '',
  deviceName: '',
  rememberMe: false,
  captchaId: '',
  captcha: '',
  captchaVerification: ''
});
const captchaEnabled = ref(false);
const captchaType = ref<'numeric' | 'slider'>('numeric');
const rememberMeEnabled = ref(true);
const captchaImage = ref<string>();
const sliderData = ref<{ originalImageBase64: string; jigsawImageBase64: string; token: string; secretKey: string }>();
const sliderOffset = ref(0);
const sliderDragging = ref(false);
const sliderVerified = ref(false);
const sliderModalOpen = ref(false);
const sliderLoading = ref(false);
const sliderSubmitting = ref(false);
const sliderClientUid = crypto.randomUUID();
const sliderPictureRef = ref<HTMLElement>();
const sliderPieceRef = ref<HTMLImageElement>();
const sliderTrackRef = ref<HTMLElement>();
let sliderStartX = 0;
const SLIDER_IMAGE_WIDTH = 310;
const SLIDER_PIECE_WIDTH = 47;
// AJ-Captcha generates block-puzzle gaps with a 5px top margin.
const SLIDER_PIECE_Y = 5;
const SLIDER_HANDLE_WIDTH = SLIDER_PIECE_WIDTH;
const sliderMax = computed(() =>
  Math.max(0, (sliderTrackRef.value?.clientWidth ?? SLIDER_IMAGE_WIDTH) - SLIDER_HANDLE_WIDTH)
);
const sliderPieceLeft = computed(() => `${sliderOffset.value}px`);

function encryptSlider(content: string, key: string) {
  return CryptoJS.AES.encrypt(content, CryptoJS.enc.Utf8.parse(key), {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7
  }).toString();
}

async function loadSliderCaptcha() {
  const response = await fetchGetSliderCaptcha(sliderClientUid);
  if (response.error || !response.data?.repData || response.data.repCode !== '0000') throw response.error;
  sliderData.value = response.data.repData;
  sliderOffset.value = 0;
  sliderVerified.value = false;
  form.captchaVerification = '';
}

async function loadCaptcha() {
  captchaLoading.value = true;
  try {
    const response = await fetchGetCaptcha();
    if (response.error || !response.data) throw response.error;
    captchaEnabled.value = response.data.enabled;
    captchaType.value = response.data.type;
    rememberMeEnabled.value = response.data.rememberMeEnabled ?? true;
    if (!rememberMeEnabled.value) form.rememberMe = false;
    captchaImage.value = response.data.image ?? undefined;
    form.captchaId = response.data.captchaId ?? '';
    form.captcha = '';
    form.captchaVerification = '';
    captchaLoadFailed.value = false;
    return true;
  } catch {
    captchaLoadFailed.value = true;
    form.captchaId = '';
    form.captcha = '';
    message.error('安全验证加载失败，请重试');
    return false;
  } finally {
    captchaLoading.value = false;
  }
}

function startSlider(event: PointerEvent) {
  if (sliderVerified.value || captchaLoading.value) return;
  sliderDragging.value = true;
  const trackLeft = sliderTrackRef.value?.getBoundingClientRect().left ?? event.clientX;
  sliderStartX = event.clientX - sliderOffset.value - trackLeft;
  sliderTrackRef.value?.setPointerCapture(event.pointerId);
}

function moveSlider(event: PointerEvent) {
  if (!sliderDragging.value) return;
  const trackLeft = sliderTrackRef.value?.getBoundingClientRect().left ?? 0;
  sliderOffset.value = Math.max(0, Math.min(sliderMax.value, event.clientX - trackLeft - sliderStartX));
}

async function finishSlider() {
  if (!sliderDragging.value || !sliderData.value || sliderSubmitting.value) return;
  sliderDragging.value = false;
  sliderSubmitting.value = true;
  const displayOffset = Math.max(0, Math.min(sliderMax.value, sliderOffset.value));
  const displayTravel = Math.max(1, sliderMax.value);
  const logicalTravel = SLIDER_IMAGE_WIDTH - SLIDER_PIECE_WIDTH;
  const logicalX = Math.round((displayOffset / displayTravel) * logicalTravel);
  const point = JSON.stringify({ x: logicalX, y: SLIDER_PIECE_Y });
  try {
    const response = await fetchCheckSliderCaptcha({
      token: sliderData.value.token,
      pointJson: encryptSlider(point, sliderData.value.secretKey),
      clientUid: sliderClientUid
    });
    if (!response.error && response.data?.repCode === '0000') {
      form.captchaVerification = encryptSlider(`${sliderData.value.token}---${point}`, sliderData.value.secretKey);
      sliderVerified.value = true;
      sliderModalOpen.value = false;
      await performLogin();
      return;
    }
    message.warning('验证未通过，请重试');
    await loadSliderCaptcha();
  } finally {
    sliderSubmitting.value = false;
  }
}

async function submit() {
  if (loading.value || captchaLoading.value || captchaLoadFailed.value || sliderSubmitting.value) return;
  await loginFormRef.value?.validate();
  if (captchaEnabled.value && captchaType.value === 'slider') {
    sliderVerified.value = false;
    sliderModalOpen.value = true;
    sliderLoading.value = true;
    try {
      await loadSliderCaptcha();
    } catch {
      sliderModalOpen.value = false;
      message.error('滑动验证加载失败，请重试');
    } finally {
      sliderLoading.value = false;
    }
    return;
  }
  await performLogin();
}

async function performLogin() {
  loading.value = true;
  await authStore.clearAuth();
  try {
    const login = await fetchLogin(form);
    if (login.error || !login.data) {
      message.error('账号或密码错误，请重新输入');
      return false;
    }
    authStore.setToken(login.data.token, form.rememberMe);
    const [profile, routes] = await Promise.all([fetchGetUserInfo(), fetchGetBackendRoutes()]);
    if (profile.error || !profile.data || routes.error || !routes.data) throw profile.error || routes.error;
    authStore.setSession(login.data.token, profile.data, routes.data);
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
    await router.replace(redirect);
    return true;
  } catch (error: unknown) {
    authStore.clearAuth();
    const apiMessage = error instanceof Error ? error.message : undefined;
    message.error(apiMessage || '登录失败，请稍后重试');
    if (captchaEnabled.value) await loadCaptcha();
    return false;
  } finally {
    loading.value = false;
  }
}
onMounted(loadCaptcha);
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
      <AForm ref="loginFormRef" :model="form" layout="vertical" @finish="submit">
        <div v-if="captchaLoadFailed" class="captcha-load-error">
          <span>安全验证加载失败，暂时无法登录</span>
          <AButton size="small" @click="loadCaptcha">重试</AButton>
        </div>
        <AFormItem label="账号" name="username" :rules="[{ required: true, message: '请输入账号' }]">
          <AInput v-model:value="form.username" autocomplete="username" size="large">
            <template #prefix><UserOutlined /></template>
          </AInput>
        </AFormItem>
        <AFormItem label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <AInputPassword v-model:value="form.password" autocomplete="current-password" size="large">
            <template #prefix><LockOutlined /></template>
          </AInputPassword>
        </AFormItem>
        <AFormItem
          v-if="captchaEnabled && captchaType === 'numeric'"
          label="验证码"
          name="captcha"
          :rules="[
            {
              required: true,
              whitespace: true,
              message: '请输入验证码'
            }
          ]"
        >
          <div class="captcha-row">
            <AInput v-model:value="form.captcha" :maxlength="4" placeholder="请输入 4 位数字验证码" />
            <AButton
              class="captcha-button"
              title="刷新验证码"
              aria-label="刷新验证码"
              :disabled="captchaLoading"
              @click="loadCaptcha"
            >
              <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="4 位数字验证码" />
              <ReloadOutlined v-else />
            </AButton>
          </div>
        </AFormItem>
        <AFormItem>
          <ACheckbox v-if="rememberMeEnabled" v-model:checked="form.rememberMe">7 天内保持登录</ACheckbox>
        </AFormItem>
        <button
          type="submit"
          class="ant-btn ant-btn-primary ant-btn-lg ant-btn-block login-submit"
          :class="{ 'ant-btn-loading': loading }"
          :disabled="loading || captchaLoading || captchaLoadFailed"
        >
          <span>登录</span>
        </button>
      </AForm>
    </section>
    <AModal
      v-model:open="sliderModalOpen"
      title="安全验证"
      :footer="null"
      :mask-closable="false"
      destroy-on-close
      wrap-class-name="slider-captcha-modal"
      @cancel="sliderModalOpen = false"
    >
      <ASpin :spinning="sliderLoading">
        <div v-if="sliderData" class="slider-captcha">
          <div ref="sliderPictureRef" class="slider-picture">
            <img :src="`data:image/png;base64,${sliderData.originalImageBase64}`" alt="滑动验证码底图" />
            <img
              ref="sliderPieceRef"
              class="slider-piece"
              :src="`data:image/png;base64,${sliderData.jigsawImageBase64}`"
              alt=""
              :style="{ left: sliderPieceLeft }"
            />
          </div>
          <div
            ref="sliderTrackRef"
            class="slider-track"
            :class="{ verified: sliderVerified }"
            @pointermove="moveSlider"
            @pointerup="finishSlider"
            @pointercancel="finishSlider"
          >
            <span>{{ sliderVerified ? '验证通过' : '向右拖动完成拼图' }}</span>
            <button type="button" class="slider-handle" :style="{ left: sliderPieceLeft }" @pointerdown="startSlider">
              {{ sliderVerified ? '✓' : '›' }}
            </button>
          </div>
        </div>
      </ASpin>
    </AModal>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--alpha-canvas);
}

.login-page::before,
.login-page::after {
  position: absolute;
  content: '';
  border-radius: 999px;
  pointer-events: none;
  filter: blur(1px);
}

.login-page::before {
  width: 720px;
  height: 720px;
  top: -420px;
  right: -190px;
  background: linear-gradient(145deg, var(--alpha-primary-soft), var(--alpha-info-soft));
}

.login-page::after {
  width: 560px;
  height: 560px;
  bottom: -390px;
  left: -190px;
  background: linear-gradient(145deg, var(--alpha-primary-soft), var(--alpha-info-soft));
}

.login-environment {
  position: absolute;
  inset: 24px 28px auto;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--alpha-primary-strong);
  font-size: 16px;
  font-weight: 600;
}

.login-environment span {
  color: var(--alpha-text-secondary);
  font-size: 13px;
  font-weight: 400;
}

.login-environment-mark {
  width: 30px;
  height: 30px;
}

.login-panel {
  position: relative;
  z-index: 4;
  width: min(100%, 448px);
  padding: 28px 24px 24px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: 8px;
  background: var(--alpha-surface);
  box-shadow: var(--alpha-shadow);
  backdrop-filter: blur(12px);
}

.login-heading {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}

.login-brand {
  width: 64px;
  height: 64px;
  flex: 0 0 auto;
}

.login-heading h1 {
  margin: 0 0 6px;
  color: var(--alpha-primary-strong);
  font-size: 24px;
  font-weight: 500;
  line-height: 1.25;
}

.login-heading p {
  margin: 0;
  color: var(--alpha-text-secondary);
  font-size: 14px;
}

.login-panel :deep(.ant-form-item) {
  margin-bottom: 18px;
}

.login-panel :deep(.ant-form-item-label) {
  padding-bottom: 6px;
}

.login-panel :deep(.ant-form-item-label > label) {
  color: var(--alpha-text);
  font-size: 14px;
}

.login-panel :deep(.ant-input-affix-wrapper),
.login-panel :deep(.ant-input) {
  border-radius: 6px;
}

.login-panel :deep(.ant-input-affix-wrapper-lg) {
  min-height: 40px;
}

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.captcha-row > :deep(.ant-input) {
  min-width: 0;
  flex: 1;
  height: 40px;
}

.captcha-question {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  padding: 0 12px;
  overflow: hidden;
  border: 1px solid var(--alpha-border-soft);
  border-radius: 6px;
  background: var(--alpha-canvas);
  color: var(--alpha-text-secondary);
  white-space: nowrap;
}

.captcha-button {
  width: 128px;
  height: 40px;
  padding: 0;
  overflow: hidden;
  border-radius: 6px;
}

.captcha-button img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slider-captcha {
  width: min(100%, 310px);
  margin: 0 auto;
  border: 1px solid var(--alpha-border-soft);
  border-radius: 8px;
  background: var(--alpha-canvas);
  box-sizing: border-box;
}

.slider-picture {
  position: relative;
  width: 100%;
  aspect-ratio: 310 / 155;
  overflow: hidden;
  border-radius: 6px;
  background: var(--alpha-canvas);
  box-shadow: 0 4px 12px rgb(15 23 42 / 12%);
}

.slider-picture > img:first-child {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slider-piece {
  position: absolute;
  top: 0;
  width: 47px;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
}

.slider-track {
  position: relative;
  height: 40px;
  margin-top: 8px;
  border: 1px solid var(--alpha-border-soft);
  border-radius: 6px;
  background: var(--alpha-canvas);
  color: var(--alpha-text-secondary);
  font-size: 13px;
  line-height: 38px;
  text-align: center;
  user-select: none;
  touch-action: none;
}

.slider-track.verified {
  border-color: var(--alpha-success);
  color: var(--alpha-success);
}

.slider-handle {
  position: absolute;
  top: -1px;
  width: 47px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--alpha-border);
  border-radius: 6px;
  background: var(--alpha-surface);
  color: var(--alpha-primary);
  cursor: grab;
  font-size: 25px;
  line-height: 36px;
  touch-action: none;
}

.slider-handle:active {
  cursor: grabbing;
}

:global(.slider-captcha-modal .ant-modal-content) {
  border: 1px solid var(--alpha-border-soft);
  border-radius: 12px;
  background: var(--alpha-surface);
  box-shadow: 0 18px 48px rgb(15 23 42 / 22%);
}

:global(.slider-captcha-modal .ant-modal-header) {
  margin-bottom: 18px;
  border-bottom: 0;
  background: transparent;
}

.captcha-load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--alpha-error) 35%, transparent);
  border-radius: 6px;
  background: var(--alpha-error-soft);
  color: var(--alpha-error-strong);
  font-size: 13px;
}

.login-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 40px;
  margin-top: 4px;
  border: 1px solid var(--alpha-primary);
  border-radius: 6px;
  background: var(--alpha-primary);
  color: var(--alpha-surface);
  cursor: pointer;
  font-size: 16px;
}

.login-submit:hover:not(:disabled) {
  border-color: var(--alpha-primary-strong);
  background: var(--alpha-primary-strong);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-environment {
    inset: 16px 18px auto;
  }

  .login-environment span {
    display: none;
  }

  .login-panel {
    padding: 24px 20px 20px;
  }

  .login-brand {
    width: 48px;
    height: 48px;
  }

  .login-heading {
    gap: 12px;
    margin-bottom: 24px;
  }

  .login-heading h1 {
    font-size: 22px;
  }
}
</style>
