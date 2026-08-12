import { request } from '../request';

export type SettingGroup =
  | 'site'
  | 'login'
  | 'file'
  | 'oauth'
  | 'payment'
  | 'security'
  | 'mini_program'
  | 'official_account';
export interface SystemSetting {
  group: SettingGroup;
  values: Record<string, unknown>;
  secretConfigured: Record<string, boolean>;
  restartRequired: boolean;
}
export interface RsaKeyPair {
  publicKey: string;
  privateKey: string;
}
export interface FileStorageCredentials {
  accessKey: string | null;
  secretKey: string | null;
}
export interface PaymentSimulationOrder {
  id: number;
  orderNo: string;
  channel: 'WECHAT' | 'ALIPAY';
  amountFen: number;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED';
  createdAt: string;
}

export const systemSettingApi = {
  publicSettings: () => request<Record<string, unknown>>({ url: '/system/settings/public', method: 'get' }),
  get: (group: SettingGroup) => request<SystemSetting>({ url: `/system/settings/${group}`, method: 'get' }),
  save: (group: SettingGroup, values: Record<string, unknown>) =>
    request<SystemSetting>({ url: `/system/settings/${group}`, method: 'put', data: { values } }),
  revealFileStorageCredentials: () =>
    request<FileStorageCredentials>({ url: '/system/settings/file/credentials', method: 'get' }),
  regenerateRsaKeys: () => request<RsaKeyPair>({ url: '/system/settings/security/keys/regenerate', method: 'post' }),
  publishOfficialAccountMenu: () =>
    request<null>({ url: '/system/settings/official-account/menu/publish', method: 'post' }),
  createSimulationOrder: (channel: 'wechat' | 'alipay', amountFen: number, idempotencyKey: string) =>
    request<PaymentSimulationOrder>({
      url: '/system/settings/pay/simulated-orders',
      method: 'post',
      data: { channel, amountFen, idempotencyKey }
    }),
  completeSimulationOrder: (id: number, status: 'succeeded' | 'failed') =>
    request<PaymentSimulationOrder>({
      url: `/system/settings/pay/simulated-orders/${id}/complete`,
      method: 'post',
      data: { status }
    })
};
