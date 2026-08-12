import { pinia } from '@/store';
import { useAuthStore } from '@/store/modules/auth';

export interface UserProfile {
  id: number;
  username: string;
  nickname?: string;
  avatar?: string;
  email?: string;
  phone?: string;
  deptId?: number;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
}

export interface AppRoute {
  id: number;
  parentId: number;
  title: string;
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON';
  path?: string;
  component?: string;
  permission?: string;
  icon?: string;
  sortOrder: number;
}

const store = useAuthStore(pinia);

export const authStore = {
  get state() {
    return {
      token: store.token,
      profile: store.profile,
      routes: store.routes
    };
  },
  getToken: () => store.token,
  setToken(token: string, _rememberMe = false) {
    store.setToken(token);
  },
  setProfile(profile: UserProfile) {
    store.setProfile(profile);
  },
  setRoutes(routes: AppRoute[]) {
    store.setRoutes(routes);
  },
  setSession(token: string, profile: UserProfile, routes: AppRoute[]) {
    store.setToken(token);
    store.setProfile(profile);
    store.setRoutes(routes);
  },
  hasPermission(permission: string) {
    return store.userInfo.buttons.some(item => item === '*' || item === permission);
  },
  clearAuth() {
    store.clearAuthState();
  }
};

export type AuthStore = Pick<typeof authStore, 'getToken' | 'clearAuth'>;
