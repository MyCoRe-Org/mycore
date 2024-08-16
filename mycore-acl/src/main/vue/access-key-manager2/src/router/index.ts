import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import AdminView from '@/views/AdminView.vue';
import ReferenceView from '@/views/ReferenceView.vue';
import { appConfig } from '@/config/provider';
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
      permissions: route.query.availablePermissions
        ? (route.query.availablePermissions as string).split(',')
        : undefined,
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
        : [],
      page: Number(route.query.page) || undefined,
      pageSize: Number(route.query.pageSize) || undefined,
    }),
    beforeEnter: (to, _from, next) => {
      const permissions = to.query.availablePermissions;
      if (!permissions || permissions.length === 0) {
        next({ name: '403' });
      } else {
        next();
      }
    },
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
