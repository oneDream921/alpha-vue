import { BACKEND_ERROR_CODE, createFlatRequest } from '@sa/axios';
import { useAuthStore } from '@/store/modules/auth';
import { getServiceBaseURL } from '@/utils/service';
import { getAuthorization, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

export const request = createFlatRequest<App.Service.Response, RequestInstanceState>(
  { baseURL },
  {
    async onRequest(config) {
      Object.assign(config.headers, { Authorization: getAuthorization() });
      return config;
    },
    isBackendSuccess(response) {
      return String(response.data.code) === import.meta.env.VITE_SERVICE_SUCCESS_CODE;
    },
    async onBackendFail(response) {
      if (String(response.data.code) === '401') {
        await useAuthStore().resetStore();
      }
      return null;
    },
    transformBackendResponse(response) {
      return response.data.data;
    },
    onError(error) {
      const headers = error.config?.headers;
      const isSilent =
        headers?.get?.('X-Client-Silent-Error') === 'true' || headers?.['X-Client-Silent-Error'] === 'true';
      if (isSilent) return;
      let message = error.message;
      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || message;
      }
      showErrorMsg(request.state, message);
    }
  }
);
