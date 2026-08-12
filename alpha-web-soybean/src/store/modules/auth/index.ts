import { computed, reactive, ref } from 'vue';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import type { LoginPayload } from '@/service/api/auth';
import { fetchGetUserInfo, fetchLogin } from '@/service/api/auth';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import type { AppRoute, UserProfile } from '@/stores/auth';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { useRouteStore } from '../route';
import { useTabStore } from '../tab';
import { clearAuthStorage, getToken } from './shared';

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref(getToken());
  const profile = ref<UserProfile | null>(null);
  const routes = ref<AppRoute[]>([]);
  const userInfo = reactive<Api.Auth.UserInfo>({ userId: '', userName: '', roles: [], buttons: [] });

  function clearAuthState() {
    clearAuthStorage();
    token.value = '';
    profile.value = null;
    routes.value = [];
    userInfo.userId = '';
    userInfo.userName = '';
    userInfo.roles = [];
    userInfo.buttons = [];
  }

  const isStaticSuper = computed(() => userInfo.roles.includes(import.meta.env.VITE_STATIC_SUPER_ROLE));
  const isLogin = computed(() => Boolean(token.value));

  function updateProfile(nextProfile: UserProfile) {
    profile.value = nextProfile;
    userInfo.userId = String(nextProfile.id);
    userInfo.userName = nextProfile.nickname || nextProfile.username;
    userInfo.roles = nextProfile.roles;
    userInfo.buttons = nextProfile.permissions;
  }

  async function resetStore() {
    clearAuthState();
    tabStore.cacheTabs();
    await routeStore.resetStore();
    await toLogin();
  }

  async function login(userNameOrPayload: string | LoginPayload, password?: string, redirect = true) {
    startLoading();
    const payload: LoginPayload =
      typeof userNameOrPayload === 'string'
        ? { username: userNameOrPayload, password: password || '' }
        : userNameOrPayload;
    const { data: loginToken, error } = await fetchLogin(payload);

    if (!error) {
      const pass = await loginByToken(loginToken);
      if (pass) {
        await redirectFromLogin(redirect);
        window.$notification?.success({
          message: $t('page.login.common.loginSuccess'),
          description: $t('page.login.common.welcomeBack', { userName: userInfo.userName })
        });
      }
    } else {
      await resetStore();
    }

    endLoading();
  }

  async function loginByToken(loginToken: { token: string }) {
    localStg.set('token', loginToken.token);
    token.value = loginToken.token;
    return getUserInfo();
  }

  async function getUserInfo() {
    const { data: info, error } = await fetchGetUserInfo();
    if (!error) {
      updateProfile(info);
      return true;
    }
    return false;
  }

  async function initUserInfo() {
    if (!getToken()) return false;
    const pass = await getUserInfo();
    if (!pass) await resetStore();
    return pass;
  }

  function setProfile(nextProfile: UserProfile) {
    updateProfile(nextProfile);
  }

  function setRoutes(nextRoutes: AppRoute[]) {
    routes.value = nextRoutes;
  }

  function setToken(nextToken: string) {
    localStg.set('token', nextToken);
    token.value = nextToken;
  }

  return {
    token,
    profile,
    routes,
    userInfo,
    isStaticSuper,
    isLogin,
    loginLoading,
    routeIsConstant: computed(() => routeStore.isInitConstantRoute),
    setToken,
    clearAuthState,
    resetStore,
    login,
    initUserInfo,
    setProfile,
    setRoutes
  };
});
