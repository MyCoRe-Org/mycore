import { App, createApp } from "vue";
import { createI18n } from "vue-i18n";
import {
  getReference,
  getWebApplicationBaseURL,
  fetchI18n,
  getAvailablePermissions,
  fetchJWT,
  fetchConfig,
} from "@/utils";
import { referenceKey, availablePermissionsKey, configKey, webApplicationBaseUrlKey, accessKeyServiceKey } from "@/keys";
import ContactManager from "@/App.vue";
import router from './router';
import { Config } from "./config";
import { AccessKeyService } from "./api/service";

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
    const authorizationHeader = (process.env.NODE_ENV === "development")
      ? `Basic ${process.env.VUE_APP_API_TOKEN}` : `Bearer ${await fetchJWT(webApplicationBaseUrl)}`
    const accessKeyService = new AccessKeyService(webApplicationBaseUrl, authorizationHeader);
    const app = createApp(ContactManager);
    app.provide(referenceKey, getReference());
    app.provide(availablePermissionsKey, getAvailablePermissions());
    app.provide(webApplicationBaseUrlKey, webApplicationBaseUrl);
    app.provide(configKey, config);
    app.provide(accessKeyServiceKey, accessKeyService);
    app.use(i18n);
    app.use(router);
    setErrorHandler(app);
    app.mount("#app");
  } catch(error) {
    router.push('/error')
  }
};

initializeApp();
