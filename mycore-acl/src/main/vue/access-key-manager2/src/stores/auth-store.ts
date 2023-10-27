import { defineStore } from 'pinia';
import { JWT, fetchJWT } from '@/utils';
import { useConfigStore } from './config-store';

interface State {
  accessToken: string | undefined
}

export const useAuthStore = defineStore('auth', {
  state: (): State => ({
    accessToken: undefined,
  }),
  actions: {
    async login(objectId: string, derivateId?: string): Promise<void> {
      const { isSessionEnabled, webApplicationBaseURL } = useConfigStore();
      try {
        const jwt: JWT = await fetchJWT(webApplicationBaseURL, objectId, isSessionEnabled, derivateId);
        if (!jwt.login_success) {
          throw new Error('noPermission');
        }
        this.accessToken = jwt.access_token;
      } catch (error) {
        throw new Error('noPermission');
      }
    },
  },
});
