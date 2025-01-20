import { BASE_URL, fetchJWT } from "@/utils";
import { defineStore } from "pinia";

interface AuthState {
  token: string | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
  }),
  actions: {
    async login(reference?: string, sessionEnabled?: boolean) {
      const jwt = await fetchJWT(BASE_URL, reference, sessionEnabled);
      this.token = jwt.access_token;
    }
  },
});