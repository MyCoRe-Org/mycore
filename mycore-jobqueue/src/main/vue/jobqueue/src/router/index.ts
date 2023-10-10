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

import {createRouter, createWebHistory} from 'vue-router'
import JobQueueListView from '../views/JobQueueListView.vue'
import JobQueueJobTableView from "@/views/JobQueueJobTableView.vue";
import JobQueue404 from "@/views/JobQueue404.vue";


function getContext() {
    const el = document.createElement('a');
    el.href = (<any>window).webApplicationBaseURL || "/";
    return el.pathname;
}



const router = createRouter({
    history: createWebHistory(getContext() + "jobqueue/"),
    routes: [
        {
            path: '/',
            name: 'JobQueueListView',
            component: JobQueueListView
        },
        {
            path: '/:queueId/:stats',
            name: 'JobQueueViewStatus',
            component: JobQueueJobTableView
        },
        {
            path: '/404',
            name: '404',
            component: JobQueue404
        }
    ]
})

export default router
