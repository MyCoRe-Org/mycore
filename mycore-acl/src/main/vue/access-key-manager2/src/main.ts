import { App, createApp } from "vue";
import { createI18n } from "vue-i18n";
import {
  getReference,
  getWebApplicationBaseURL,
  fetchI18n,
  getAvailablePermissions,
  fetchConfig,
} from "@/utils";
import { referenceKey, availablePermissionsKey, configKey, webApplicationBaseUrlKey } from "@/keys";
import ContactManager from "@/App.vue";
import router from './router';
import { Config } from "./config";
import { createPinia } from 'pinia';

const ALLOWED_SESSION_TYPES = "MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes";
const webApplicationBaseUrl = getWebApplicationBaseURL() as string;

const initializeI18n = async () => {
  const data = await fetchI18n(webApplicationBaseUrl);
  return createI18n({
    legacy: false,
    locale: "_",
    messages: {
      _: data,
    },
    warnHtmlInMessage: "off",
  });
}

const initializeConfig = async (): Promise<Config> => {
  const data = await fetchConfig(webApplicationBaseUrl);
  return {
    isSessionEnabled: data[ALLOWED_SESSION_TYPES] !== undefined && data[ALLOWED_SESSION_TYPES].length > 0,
    allowedSessionPermissionTypes: data[ALLOWED_SESSION_TYPES].split(","),
  } as Config;
}

const setErrorHandler = (app: App) => {
  app.config.errorHandler = (err, instance, info) => {
    // eslint-disable-next-line
    console.error('Global error:', err);
    // eslint-disable-next-line
    console.log('Vue instance:', instance);
    // eslint-disable-next-line
    console.log('Error info:', info);
  };
}

const initializeApp = async () => {
  try {
    const i18n = await initializeI18n();
    const config = initializeConfig();
    const app = createApp(ContactManager);
    app.provide(referenceKey, getReference());
    const pinia = createPinia();
    app.provide(availablePermissionsKey, getAvailablePermissions());
    app.provide(webApplicationBaseUrlKey, webApplicationBaseUrl);
    app.provide(configKey, config);
    app.use(i18n);
    app.use(router);
    app.use(pinia);
    setErrorHandler(app);
    app.mount("#app");
  } catch(error) {
    router.push('/error')
  }
};

initializeApp();
