import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import { ModalPlugin, PaginationPlugin } from 'bootstrap-vue';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import { library } from '@fortawesome/fontawesome-svg-core';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useConfigStore, useApplicationStore } from '@/stores';
import {
  faEye,
  faTimes,
  faPlus,
  faInfoCircle,
  faTrash,
  faSave,
  faRandom,
  faAngleLeft,
} from '@fortawesome/free-solid-svg-icons';
import { createPinia } from 'pinia';
import {
  getObjectId,
  getDerivateId,
  getWebApplicationBaseURL,
  fetchI18n,
} from '@/utils';
import ContactManager from '@/App.vue';

library.add(faEye, faTimes, faPlus, faInfoCircle, faTrash, faSave, faRandom, faAngleLeft);

const webApplicationBaseURL = getWebApplicationBaseURL() as string;
const objectId = getObjectId();
const derivateId = getDerivateId();

(async () => {
  const data = await fetchI18n(webApplicationBaseURL);
  const i18n = createI18n({
    locale: '_',
    messages: {
      _: data,
    },
  });
  const app = createApp(ContactManager);
  app.use(createPinia());
  const configStore = useConfigStore();
  configStore.webApplicationBaseURL = webApplicationBaseURL;
  const applicationStore = useApplicationStore();
  applicationStore.objectId = objectId;
  applicationStore.derivateId = derivateId;
  app.use(i18n);
  app.use(ModalPlugin);
  app.use(PaginationPlugin);
  app.component('font-awesome-icon', FontAwesomeIcon);
  app.config.errorHandler = (err, instance, info) => {
    // eslint-disable-next-line
    console.error('Global error:', err);
    // eslint-disable-next-line
    console.log('Vue instance:', instance);
    // eslint-disable-next-line
    console.log('Error info:', info);
  };
  app.mount('#app');
})();
