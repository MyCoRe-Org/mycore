import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import GenericErrorView from '@/views/GenericErrorView.vue';
import NotAuthorizedView from '@/views/NotAuthorizedView.vue';


const routes: Array<RouteRecordRaw> = [
  {
    path: '/401',
    component: NotAuthorizedView
  },
  {
    path: '/',
    component: HomeView,
  },
  {
    path: '/error',
		component: GenericErrorView,
	}
]

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;