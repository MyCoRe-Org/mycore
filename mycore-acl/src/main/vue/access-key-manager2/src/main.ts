import { App, createApp } from 'vue';
import { createI18n, I18n } from 'vue-i18n';
import { BASE_URL, fetchTranslations } from '@/common/utils';
import ContactManager from '@/App.vue';
import router from './router';
import './style.css';
if (import.meta.env.MODE === 'development') {
  import('bootstrap/dist/css/bootstrap.min.css');
  import('font-awesome/css/font-awesome.min.css');
}

const APP_ID = 'app';

const initI18n = async (baseUrl: string): Promise<I18n> => {
  const data = await fetchTranslations(baseUrl);
  return createI18n({
    legacy: false,
    locale: '_',
    messages: {
      _: data,
    },
    warnHtmlInMessage: 'off',
  });
};

const setErrorHandler = (app: App): void => {
  app.config.errorHandler = (err, instance, info) => {
    console.error('Global error:', err);
    console.log('Vue instance:', instance);
    console.log('Error info:', info);
  };
};

const initApp = async (): Promise<void> => {
  try {
    const i18n = await initI18n(BASE_URL);
    const app = createApp(ContactManager);
    app.use(i18n);
    app.use(router);
    setErrorHandler(app);
    app.mount(`#${APP_ID}`);
  } catch (error) {
    const container = document.getElementById(APP_ID);
    if (container) {
      container.innerHTML = '<p>Failed to initialize app.</p>';
    }
    console.error('App initialization failed:', error);
  }
};

initApp();
