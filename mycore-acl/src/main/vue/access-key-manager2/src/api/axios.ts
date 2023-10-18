import axios from 'axios';
import { getWebApplicationBaseURL } from '@/utils';
import { useAuthStore } from '@/stores/auth-store';

export type ErrorResponse = {
  detail: string;
  errorCode: string;
  message: string;
  timestamp: string;
  uuid: string;
}

const instance = axios.create({
  baseURL: getWebApplicationBaseURL(),
});

instance.interceptors.request.use((config) => {
  const tmp = config;
  if (tmp.headers === undefined) {
    tmp.headers = { };
  }
  if (process.env.NODE_ENV === 'development') {
    tmp.headers.Authorization = `Basic ${process.env.VUE_APP_API_TOKEN}`;
  } else if (process.env.NODE_ENV === 'production') {
    const { accessToken } = useAuthStore();
    if (accessToken) {
      tmp.headers.Authorization = `Bearer ${accessToken}`;
    }
  }
  return tmp;
});

instance.interceptors.response.use((response) => response, (error) => {
  if (axios.isAxiosError(error) && error.response) {
    if (error.response.status === 401 || error.response.status === 403) {
      return Promise.reject(new Error('noPermission'));
    }
    if (error.response.data) {
      const { errorCode } = (error.response.data as ErrorResponse);
      if (errorCode && errorCode !== 'UNKNOWN') {
        return Promise.reject(new Error(errorCode));
      }
    }
  }
  return Promise.reject(new Error('fatal'));
});

export default instance;
