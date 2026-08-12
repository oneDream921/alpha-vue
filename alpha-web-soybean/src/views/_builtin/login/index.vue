<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  CloseCircleOutlined,
  HolderOutlined,
  InfoCircleOutlined,
  LockOutlined,
  ReloadOutlined,
  UserOutlined
} from '@ant-design/icons-vue';
import { fetchGetBackendRoutes } from '@/service/api/route';
import { fetchGetCaptcha, fetchGetUserInfo, fetchLogin } from '@/service/api/auth';
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
  captcha: ''
});
const captchaEnabled = ref(false);
const captchaType = ref<'numeric' | 'slider'>('numeric');
const rememberMeEnabled = ref(true);
const captchaImage = ref<string>();
const captchaQuestion = ref<string>();
const sliderBackground = ref<string>();
const sliderPiece = ref<string>();
const sliderWidth = ref(420);
const sliderHeight = ref(280);
const sliderPieceWidth = ref(42);
const sliderPieceTop = ref(42);
const sliderOffset = ref(0);
const sliderDragging = ref(false);
const sliderStartX = ref(0);
const sliderStartOffset = ref(0);
const sliderModalOpen = ref(false);
const sliderTrack = ref<SliderElement>();
const sliderHandle = ref<SliderElement>();
const sliderTrace = ref<Array<{ x: number; y: number; t: number }>>([]);
const sliderStartedAt = ref(0);
const sliderStatus = ref('');
const sliderMaxOffset = computed(() => Math.max(0, sliderWidth.value - sliderPieceWidth.value));
const sliderProgress = computed(() => (sliderMaxOffset.value > 0 ? sliderOffset.value / sliderMaxOffset.value : 0));

async function loadCaptcha() {
  captchaLoading.value = true;
  try {
    const response = await fetchGetCaptcha();
    if (response.error || !response.data) throw response.error;
    captchaEnabled.value = response.data.enabled;
    captchaType.value = response.data.type ?? 'numeric';
    rememberMeEnabled.value = response.data.rememberMeEnabled ?? true;
    if (!rememberMeEnabled.value) form.rememberMe = false;
    captchaImage.value = response.data.image ?? undefined;
    captchaQuestion.value = response.data.question ?? undefined;
    sliderBackground.value = response.data.sliderBackground ?? undefined;
    sliderPiece.value = response.data.sliderPiece ?? undefined;
    sliderWidth.value = response.data.sliderWidth ?? 420;
    sliderHeight.value = response.data.sliderHeight ?? 280;
    sliderPieceWidth.value = response.data.sliderPieceWidth ?? 42;
    sliderPieceTop.value = response.data.sliderPieceTop ?? 42;
    sliderOffset.value = 0;
    sliderTrace.value = [];
    sliderStartedAt.value = 0;
    sliderStatus.value = '';
    form.captchaId = response.data.captchaId ?? '';
    form.captcha = '';
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

type SliderPointerEvent = {
  clientX: number;
  clientY: number;
  currentTarget: unknown;
  pointerId: number;
};

type SliderElement = {
  getBoundingClientRect: () => {
    left: number;
    top: number;
    width: number;
  };
};

function startSlider(event: SliderPointerEvent) {
  if (!sliderPiece.value || captchaLoading.value || loading.value) return;
  sliderStatus.value = '正在验证';
  sliderDragging.value = true;
  sliderStartedAt.value = Date.now();
  sliderStartX.value = event.clientX;
  sliderStartOffset.value = sliderOffset.value;
  sliderTrace.value = [];
  recordSliderPoint(event);
  (
    event.currentTarget as {
      setPointerCapture: (pointerId: number) => void;
    }
  ).setPointerCapture(event.pointerId);
}

function moveSlider(event: SliderPointerEvent) {
  if (!sliderDragging.value) return;
  const trackWidth = sliderTrack.value?.getBoundingClientRect().width ?? 0;
  const handleWidth = sliderHandle.value?.getBoundingClientRect().width ?? 50;
  const displayTravel = Math.max(1, trackWidth - handleWidth);
  const logicalDelta = ((event.clientX - sliderStartX.value) / displayTravel) * sliderMaxOffset.value;
  sliderOffset.value = Math.min(sliderMaxOffset.value, Math.max(0, sliderStartOffset.value + logicalDelta));
  recordSliderPoint(event);
}

function recordSliderPoint(event: SliderPointerEvent, elapsed = Date.now() - sliderStartedAt.value, force = false) {
  const bounds = sliderTrack.value?.getBoundingClientRect();
  if (!bounds) return;
  const y = Math.max(0, Math.min(48, event.clientY - bounds.top));
  const last = sliderTrace.value.at(-1);
  if (!last || force || elapsed - last.t >= 16) {
    sliderTrace.value.push({ x: sliderOffset.value, y, t: elapsed });
  }
}

async function endSlider() {
  if (!sliderDragging.value) return;
  sliderDragging.value = false;
  const lastTraceTime = sliderTrace.value.at(-1)?.t ?? -1;
  const duration = Math.max(Date.now() - sliderStartedAt.value, lastTraceTime + 1);
  recordSliderPoint(
    {
      clientX: sliderStartX.value + sliderOffset.value,
      clientY: 0,
      currentTarget: sliderTrack.value,
      pointerId: 0
    },
    duration,
    true
  );
  form.captcha = `${Math.round(sliderOffset.value)}~${duration}~${sliderTrace.value
    .map(point => `${Math.round(point.x)},${Math.round(point.y)},${point.t}`)
    .join(';')}`;
  sliderStatus.value = '正在校验';
  const success = await performLogin();
  if (success) {
    sliderModalOpen.value = false;
  } else if (captchaEnabled.value && captchaType.value === 'slider' && !captchaLoadFailed.value) {
    sliderModalOpen.value = true;
    sliderStatus.value = '验证未通过，请重试';
  }
}

async function refreshSlider() {
  if (await loadCaptcha()) sliderModalOpen.value = true;
}

function closeSlider() {
  sliderModalOpen.value = false;
  sliderStatus.value = '';
  form.captcha = '';
}

function showSliderInfo() {
  message.info('请拖动圆形滑块，使拼图位置对齐');
}

async function submit() {
  if (captchaLoading.value || captchaLoadFailed.value) return;
  await loginFormRef.value?.validate();
  if (captchaEnabled.value && captchaType.value === 'slider' && !form.captcha) {
    sliderModalOpen.value = true;
    return;
  }
  await performLogin();
}

async function performLogin() {
  loading.value = true;
  await authStore.clearAuth();
  try {
    const login = await fetchLogin(form);
    if (login.error || !login.data) throw login.error;
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
    message.error(apiMessage || '登录失败，请检查账号和密码');
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
      <AForm ref="loginFormRef" :model="form" layout="vertical" @finish="submit" @keyup.enter="submit">
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
          <div v-if="captchaType === 'numeric'" class="captcha-row">
            <div v-if="captchaQuestion" class="captcha-question">
              {{ captchaQuestion }}
            </div>
            <AInput v-model:value="form.captcha" :maxlength="16" placeholder="请输入验证码" />
            <AButton
              class="captcha-button"
              title="刷新验证码"
              aria-label="刷新验证码"
              :disabled="captchaLoading"
              @click="loadCaptcha"
            >
              <img v-if="captchaImage && !captchaLoading" :src="captchaImage" alt="验证码" />
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
          @click="submit"
        >
          <span>登录</span>
        </button>
      </AForm>
      <AModal
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
              aspectRatio: `${sliderWidth} / ${sliderHeight}`
            }"
          >
            <img :src="sliderBackground" alt="滑块验证码背景" />
            <img
              class="slider-captcha-piece"
              :src="sliderPiece"
              alt="滑块拼图"
              :style="{
                width: `${(sliderPieceWidth / sliderWidth) * 100}%`,
                height: `${(sliderPieceWidth / sliderHeight) * 100}%`,
                top: `${(sliderPieceTop / sliderHeight) * 100}%`,
                left: `${(sliderOffset / sliderWidth) * 100}%`
              }"
            />
          </div>
          <div ref="sliderTrack" class="slider-captcha-track">
            <div
              class="slider-captcha-fill"
              :style="{
                width: `${sliderProgress * 100}%`
              }"
            />
            <span id="slider-captcha-status" aria-live="polite">
              {{ sliderStatus || '拖动滑块完成验证' }}
            </span>
            <button
              ref="sliderHandle"
              type="button"
              class="slider-captcha-handle"
              aria-label="拖动滑块"
              :disabled="captchaLoading || loading"
              :style="{
                left: `${sliderProgress * 100}%`,
                transform: `translateX(-${sliderProgress * 100}%)`
              }"
              @pointerdown.stop="startSlider"
            >
              <HolderOutlined />
            </button>
          </div>
          <div class="slider-captcha-toolbar">
            <button type="button" title="关闭验证" aria-label="关闭验证" @click="closeSlider">
              <CloseCircleOutlined />
            </button>
            <button
              type="button"
              title="刷新验证图片"
              aria-label="刷新验证图片"
              :disabled="captchaLoading || loading"
              @click="refreshSlider"
            >
              <ReloadOutlined />
            </button>
            <button type="button" title="验证说明" aria-label="验证说明" @click="showSliderInfo">
              <InfoCircleOutlined />
            </button>
            <span class="slider-captcha-brand">本地安全验证</span>
          </div>
        </div>
      </AModal>
    </section>
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
}

.captcha-row > :deep(.ant-input) {
  min-width: 0;
  flex: 1;
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
  width: 92px;
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
