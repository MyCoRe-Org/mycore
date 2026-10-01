/*
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

import { createRouter, createWebHistory } from 'vue-router'
import { getWebApplicationBaseURL } from "@/api/BaseURL";
import DuplicateListView from "@/views/DuplicateListView.vue";
import NoDuplicateListView from "@/views/NoDuplicateListView.vue";

export function getContext(): string {
  if (import.meta.env.DEV) {
    return import.meta.env.BASE_URL;
  }
  const el = document.createElement('a');
  el.href = getWebApplicationBaseURL();
  return el.pathname + "dedup/";
}

const router = createRouter({
  history: createWebHistory(getContext()),
  routes: [
    {
      path: '/',
      name: 'duplicates',
      component: DuplicateListView,
    },
    {
      path: '/no-duplicates',
      name: 'noDuplicates',
      component: NoDuplicateListView,
    }
  ],
})

export default router
