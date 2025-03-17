/*!
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

import { App, createApp } from 'vue';
import ContactManager from '@/App.vue';
import router from './router';
import { createI18n } from 'vue-i18n';
import { LangService } from '@mycore-test/js-common/i18n';
import { appConfig, accessKeyConfig } from './common/config';
import {
  UnauthorizedActionError,
  PermissionError,
} from '@mycore-test/js-common/utils/errors';
if (import.meta.env.DEV) {
  import('bootstrap/dist/css/bootstrap.min.css');
  import('font-awesome/css/font-awesome.min.css');
}
import '@mycore-org/vue-access-key-manager/dist/vue-access-key-manager.css';

const I18N_PREFIX = 'component.acl.accesskey.*';
const APP_ID = 'app';

const setErrorHandler = (app: App): void => {
  app.config.errorHandler = (error, instance, info) => {
    console.log('Vue instance:', instance);
    console.log('Error info:', info);
    console.log('Error:', error);
    if (error instanceof UnauthorizedActionError) {
      router.push({ name: '401' });
    } else if (error instanceof PermissionError) {
      router.push({ name: '403' });
    } else {
      // TODO create error view
      router.push({ name: 'error' });
    }
  };
};

// TODO validate config
const langService = new LangService(appConfig.baseUrl);

const initApp = async () => {
  try {
    const [translations] = await Promise.all([
      langService.getTranslations(I18N_PREFIX, appConfig.currentLang),
    ]);
    const i18n = createI18n({
      legacy: false,
      locale: appConfig.currentLang,
      messages: { [appConfig.currentLang]: translations },
    });
    const app = createApp(ContactManager);
    app.use(i18n);
    app.use(router);
    app.provide('appConfig', appConfig);
    app.provide('accessKeyConfig', accessKeyConfig);
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
