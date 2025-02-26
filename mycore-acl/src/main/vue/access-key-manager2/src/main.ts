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
import { BASE_URL, CURRENT_LANG, fetchAccessKeyConfig } from '@/common/config';
import { LangService } from '@mycore-test/js-common/i18n';
if (import.meta.env.DEV) {
  import('bootstrap/dist/css/bootstrap.min.css');
  import('font-awesome/css/font-awesome.min.css');
}

const I18N_PREFIX = 'component.acl.accesskey.*';
const APP_ID = 'app';

const setErrorHandler = (app: App): void => {
  app.config.errorHandler = (err, instance, info) => {
    console.error('Global error:', err);
    console.log('Vue instance:', instance);
    console.log('Error info:', info);
  };
};

const langService = new LangService(BASE_URL);

const initApp = async () => {
  try {
    const [config, translations] = await Promise.all([
      fetchAccessKeyConfig(BASE_URL),
      langService.getTranslations(I18N_PREFIX, CURRENT_LANG),
    ]);
    const i18n = createI18n({
      legacy: false,
      locale: CURRENT_LANG,
      messages: { [CURRENT_LANG]: translations },
    });
    const app = createApp(ContactManager);
    app.use(i18n);
    // TODO fix 401 and 403
    app.use(router);
    app.provide('accessKeyConfig', config);
    app.provide('baseUrl', BASE_URL);
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
