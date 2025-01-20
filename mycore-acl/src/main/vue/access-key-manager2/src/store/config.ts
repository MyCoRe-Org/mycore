import { defineStore } from "pinia";
import { Config } from "@/config";
import { BASE_URL, fetchConfig } from "@/utils";

interface ConfigState {
  config: Config;
}

export const useConfigStore = defineStore('config', {
  state: (): ConfigState => ({
    config: {
      isSessionEnabled: false,
      allowedSessionPermissionTypes: [],
    },
  }),
  actions: {
    async loadConfig() {
      this.config = await fetchConfig(BASE_URL);
    }
  },
});