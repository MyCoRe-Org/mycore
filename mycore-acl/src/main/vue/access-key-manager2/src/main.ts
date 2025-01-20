import { App, createApp } from "vue";
import { createI18n, I18n } from "vue-i18n";
import { BASE_URL, fetchTranslations } from "@/utils";
import ContactManager from "@/App.vue";
import router from './router';
import { createPinia } from 'pinia';

const initI18n = async (baseUrl: string): Promise<I18n> => {
  const data = await fetchTranslations(baseUrl);
  return createI18n({
    legacy: false,
    locale: "_",
    messages: {
      _: data,
    },
    warnHtmlInMessage: "off",
  });
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

const initApp = async (): Promise<void> => {
  try {
    const i18n = await initI18n(BASE_URL);
    const app = createApp(ContactManager);
    const pinia = createPinia();
    app.use(i18n);
    app.use(router);
    app.use(pinia);
    setErrorHandler(app);
    app.mount("#app");
  } catch(error) {
    const container = document.getElementById("app");
    if (container) {
      container.innerHTML = '<p>Failed to initialize app.</p>';
    }
    // eslint-disable-next-line
    console.error('App initialization failed:', error);
  }
};

initApp();
