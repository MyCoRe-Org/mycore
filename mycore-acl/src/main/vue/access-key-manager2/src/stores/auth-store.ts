import { defineStore } from "pinia";
import { JWT, fetchJWT } from "@/utils";
import { useConfigStore } from "./config-store";

export interface State {
  accessToken: string | undefined;
}

export const useAuthStore = defineStore("auth", {
  state: (): State => ({
    accessToken: undefined,
  }),
  actions: {
    async login(reference?: string): Promise<void> {
      const { isSessionEnabled, webApplicationBaseURL } = useConfigStore();
      try {
        const jwt: JWT = reference
          ? await fetchJWT(webApplicationBaseURL)
          : await fetchJWT(webApplicationBaseURL, reference, isSessionEnabled);
        if (!jwt.login_success) {
          throw new Error("component.acl.accesskey.frontend.error.noPermission");
        }
        this.accessToken = jwt.access_token;
      } catch (error) {
        throw new Error("component.acl.accesskey.frontend.error.noPermission");
      }
    },
  },
});
