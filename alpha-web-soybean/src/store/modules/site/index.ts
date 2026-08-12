import { ref } from 'vue';
import { defineStore } from 'pinia';
import { fileApi } from '@/service/api/files';
import { systemSettingApi } from '@/service/api/settings';
import { SetupStoreId } from '@/enum';

const DEFAULT_SITE_NAME = 'Alpha Vue 管理系统';
const DEFAULT_COPYRIGHT = `Copyright MIT © ${new Date().getFullYear()} Alpha Vue`;

function fileId(value: string) {
  const match = value.match(/(?:file:|\/files\/)(\d+)/);
  return match ? Number(match[1]) : undefined;
}

export const useSiteStore = defineStore(SetupStoreId.Site, () => {
  const siteName = ref(DEFAULT_SITE_NAME);
  const siteDescription = ref('');
  const copyright = ref(DEFAULT_COPYRIGHT);
  const icp = ref('');
  const siteLogoUrl = ref('');
  const watermarkEnabled = ref(false);
  const watermarkType = ref<'custom' | 'username'>('custom');
  const watermarkContent = ref('');
  const watermarkOpacity = ref(0.12);
  let siteLogoObjectUrl: string | undefined;

  function clearLogoObjectUrl() {
    if (siteLogoObjectUrl) URL.revokeObjectURL(siteLogoObjectUrl);
    siteLogoObjectUrl = undefined;
  }

  async function resolveLogo(value: unknown) {
    clearLogoObjectUrl();
    siteLogoUrl.value = '';
    const source = typeof value === 'string' ? value.trim() : '';
    if (!source) return;

    try {
      const id = fileId(source);
      const accessResponse = id ? await fileApi.accessUrl(id) : null;
      const accessUrl = id ? accessResponse?.data : source;
      if (!accessUrl) return;
      siteLogoUrl.value = accessUrl;
    } catch {
      // A site logo is optional; keep the branded fallback when it cannot be read.
    }
  }

  async function refresh() {
    try {
      const response = await systemSettingApi.publicSettings();
      const site = (response.data?.site ?? {}) as Record<string, unknown>;
      siteName.value = String(site.siteName || DEFAULT_SITE_NAME);
      siteDescription.value = String(site.siteDescription || '');
      copyright.value = String(site.copyright || DEFAULT_COPYRIGHT);
      icp.value = String(site.icp || '');
      watermarkEnabled.value = site.watermarkEnabled === true;
      watermarkType.value = site.watermarkType === 'username' ? 'username' : 'custom';
      watermarkContent.value = String(site.watermarkContent || '');
      const opacity = Number(site.watermarkOpacity);
      watermarkOpacity.value = Number.isFinite(opacity) ? Math.min(1, Math.max(0, opacity)) : 0.12;
      await resolveLogo(site.siteLogo);
    } catch {
      // Public settings are optional for the shell; defaults remain usable.
    }
  }

  return {
    siteName,
    siteDescription,
    copyright,
    icp,
    siteLogoUrl,
    watermarkEnabled,
    watermarkType,
    watermarkContent,
    watermarkOpacity,
    refresh
  };
});
