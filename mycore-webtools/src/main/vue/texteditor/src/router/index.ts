import {createRouter, createWebHistory} from 'vue-router'
import NotFoundView from "@/views/NotFoundView.vue";
import TextEditorView from "@/views/TextEditorView.vue";

export function getContext(): string {
    if (import.meta.env.DEV) {
        return import.meta.env.BASE_URL
    }
    const el = document.createElement('a');
    el.href = getWebApplicationBaseURL();
    return el.pathname + "modules/webtools/texteditor/";
}

export function getMCRApplicationBaseURL(): string {
    if (import.meta.env.DEV) {
        return "http://localhost:8291/mir/"
    }
    return getWebApplicationBaseURL();
}

export function getWebApplicationBaseURL(): string {
    if (import.meta.env.DEV) {
        return import.meta.env.BASE_URL;
    }
    if ((<any>window).mycore.webApplicationBaseURL) {
        return (<any>window).mycore.webApplicationBaseURL;
    }
    throw "Fatal error: 'mycore.webApplicationBaseURL' is not set.";
}

const router = createRouter({
    history: createWebHistory(getContext()),
    routes: [
        {
            path: '/:type/:id*',
            name: 'TextEditor',
            component: TextEditorView,
            props: (route) => {
                return {
                    type: route.params.type,
                    id: typeof route.params.id === "string" ? route.params.id : route.params.id.join("/")
                }
            }
        },
        {
            path: '/:catchAll(.*)',
            name: 'NotFound',
            component: NotFoundView
        }
    ]
})

export default router

