/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import Vue from 'vue';
import vueCustomElement from 'vue-custom-element';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import {
  ButtonPlugin,
  PaginationPlugin,
  TablePlugin,
  LayoutPlugin,
  AlertPlugin,
  OverlayPlugin,
  ModalPlugin,
  FormInputPlugin,
  FormSelectPlugin,
  InputGroupPlugin,
  FormGroupPlugin,
  LinkPlugin,
  PopoverPlugin,
  FormTextareaPlugin,
  FormPlugin,
  FormCheckboxPlugin,
  FormDatepickerPlugin,
} from 'bootstrap-vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import {
  faEdit,
  faTimes,
  faPlus,
  faInfoCircle,
  faTrash,
  faSave,
  faRandom,
  faAngleLeft,
} from '@fortawesome/free-solid-svg-icons';
import AccessKeyManager from './App.vue';

Vue.config.productionTip = false;

Vue.use(vueCustomElement);

Vue.use(ButtonPlugin);
Vue.use(PaginationPlugin);
Vue.use(TablePlugin);
Vue.use(LayoutPlugin);
Vue.use(AlertPlugin);
Vue.use(OverlayPlugin);
Vue.use(ModalPlugin);
Vue.use(FormInputPlugin);
Vue.use(FormSelectPlugin);
Vue.use(InputGroupPlugin);
Vue.use(FormGroupPlugin);
Vue.use(FormCheckboxPlugin);
Vue.use(FormDatepickerPlugin);
Vue.use(LinkPlugin);
Vue.use(FormPlugin);
Vue.use(PopoverPlugin);
Vue.use(FormTextareaPlugin);

library.add(faEdit, faTimes, faPlus, faInfoCircle, faTrash, faSave, faRandom, faAngleLeft);
Vue.component('font-awesome-icon', FontAwesomeIcon);

Vue.customElement('access-key-manager', new AccessKeyManager().$options);
