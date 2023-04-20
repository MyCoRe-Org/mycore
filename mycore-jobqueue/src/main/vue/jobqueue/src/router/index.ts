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
