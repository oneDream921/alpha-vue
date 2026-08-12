import type { UserProfile } from '@/stores/auth';
import { request } from '../request';

export interface LoginPayload {
  username: string;
  password: string;
  deviceId?: string;
  deviceName?: string;
  captcha?: string;
  captchaId?: string;
  captchaVerification?: string;
  rememberMe?: boolean;
}

export interface LoginResult {
  token: string;
  tokenType: string;
  expiresIn: number;
}

export interface CaptchaResult {
  enabled: boolean;
  type: 'numeric' | 'slider';
  rememberMeEnabled: boolean;
  captchaId: string | null;
  image: string | null;
}

export interface SliderCaptchaData {
  originalImageBase64: string;
  jigsawImageBase64: string;
  token: string;
  secretKey: string;
}

interface AjCaptchaResponse<T = unknown> {
  repCode: string;
  repMsg: string;
  repData: T;
}

export function fetchLogin(payload: LoginPayload) {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    headers: { 'X-Client-Silent-Error': 'true' },
    data: { ...payload, clientId: 'pc-admin' }
  });
}

export function fetchGetCaptcha() {
  return request<CaptchaResult>({ url: '/auth/captcha' });
}

export function fetchGetSliderCaptcha(clientUid: string) {
  return request<AjCaptchaResponse<SliderCaptchaData>>({
    url: '/auth/captcha/slider/get',
    method: 'post',
    data: { captchaType: 'blockPuzzle', clientUid }
  });
}

export function fetchCheckSliderCaptcha(data: { token: string; pointJson: string; clientUid: string }) {
  return request<AjCaptchaResponse>({
    url: '/auth/captcha/slider/check',
    method: 'post',
    data: { captchaType: 'blockPuzzle', ...data }
  });
}

export function fetchGetUserInfo() {
  return request<UserProfile>({ url: '/auth/profile' });
}

export function fetchLogout() {
  return request<null>({ url: '/auth/logout', method: 'post' });
}

export function fetchUpdateProfile(data: { nickname: string; avatar?: string; email?: string; phone?: string }) {
  return request<UserProfile>({ url: '/auth/profile', method: 'put', data });
}

export function fetchUploadAvatar(file: File) {
  const data = new FormData();
  data.append('file', file);
  return request<UserProfile>({ url: '/auth/avatar', method: 'post', data });
}

export function fetchChangePassword(data: { currentPassword: string; newPassword: string }) {
  return request<null>({ url: '/auth/password', method: 'put', data });
}

export function fetchCustomBackendError(code: string, message: string) {
  return request({ url: '/auth/error', params: { code, message } });
}
