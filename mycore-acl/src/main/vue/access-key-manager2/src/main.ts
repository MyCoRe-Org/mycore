import { App, createApp } from 'vue';
import router from '@/router';
import ContactManager from '@/App.vue';
import { createI18n } from 'vue-i18n';
import { LangApiClient } from '@jsr/mycore__js-common/i18n';
import { appConfig, accessKeyConfig, I18N_PREFIX } from '@/config/provider';
import {
  UnauthorizedActionError,
  PermissionError,
} from '@jsr/mycore__js-common/utils/errors';
import { AuthStrategy } from '@jsr/mycore__js-common/auth';
import { createAccessKeyManager } from '@mycore-org/vue-access-key-manager';

if (import.meta.env.DEV) {
  import('bootstrap/dist/css/bootstrap.min.css');
  import('font-awesome/css/font-awesome.min.css');
}
import '@mycore-org/vue-access-key-manager/dist/vue-access-key-manager.css';

const APP_ID = 'app';

const devAuthStrategy: AuthStrategy | undefined = import.meta.env.DEV
  ? new (class implements AuthStrategy {
      async getHeaders(): Promise<Record<string, string>> {
        return {
          Authorization: `Basic ${import.meta.env.VITE_APP_API_TOKEN}`,
        };
      }
    })()
  : undefined;

const setErrorHandler = (app: App): void => {
  app.config.errorHandler = error => {
    if (error instanceof UnauthorizedActionError) {
      router.push({ name: '401' });
    } else if (error instanceof PermissionError) {
      router.push({ name: '403' });
    } else {
      router.push({ name: 'error' });
    }
  };
};

const langClient = new LangApiClient(appConfig.baseUrl);

const initApp = async () => {
  try {
    const [vueTranslations, accessKeyTranslations] = await Promise.all([
      langClient.getTranslations(
        'component.webtools.vue.*',
        appConfig.currentLang
      ),
      langClient.getTranslations(I18N_PREFIX, appConfig.currentLang),
    ]);
    const translations: Record<string, string> = {
      ...vueTranslations,
      ...accessKeyTranslations,
    };
    const i18n = createI18n({
      legacy: false,
      locale: appConfig.currentLang,
      messages: { [appConfig.currentLang]: translations },
    });
    const app = createApp(ContactManager);
    app.use(i18n);
    app.use(router);
    app.use(
      createAccessKeyManager({
        baseUrl: appConfig.baseUrl,
        authStrategy: devAuthStrategy,
        accessKeyConfig: {
          allowedSessionPermissions:
            accessKeyConfig.allowedAccessKeySessionPermissions,
        },
      })
    );
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
