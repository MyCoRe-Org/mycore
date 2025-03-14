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

import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AppView from '@/views/AppView.vue';
import { Error401View, Error403View } from '@mycore-org/vue-components';
import { appConfig } from '@/common/config';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: AppView,
  },
  {
    path: '/401',
    name: '401',
    component: Error401View,
  },
  {
    path: '/403',
    name: '403',
    component: Error403View,
  },
];

const getContext = (): string => {
  if (import.meta.env.DEV) {
    return '';
  }
  const el = document.createElement('a');
  el.href = appConfig.baseUrl;
  return `${el.pathname}access-key-manager`;
};

const router = createRouter({
  history: createWebHistory(getContext()),
  routes,
});

export default router;
