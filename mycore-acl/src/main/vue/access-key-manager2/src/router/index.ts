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
import AdminView from '@/views/AdminView.vue';
import ReferenceView from '@/views/ReferenceView.vue';
import { appConfig } from '@/common/config';
import { errorRoutes } from '@mycore-org/vue-components';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/admin',
  },
  {
    path: '/admin/:reference?',
    component: AdminView,
    props: route => ({
      reference: route.params.reference || undefined,
      page: Number(route.query.page) || undefined,
      pageSize: Number(route.query.pageSize) || undefined,
    }),
  },
  {
    path: '/:reference',
    component: ReferenceView,
    props: route => ({
      reference: route.params.reference || undefined,
      permissions: route.query.availablePermissions
        ? (route.query.availablePermissions as string).split(',')
        : undefined,
      page: Number(route.query.page) || undefined,
      pageSize: Number(route.query.pageSize) || undefined,
    }),
  },
  ...errorRoutes,
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
