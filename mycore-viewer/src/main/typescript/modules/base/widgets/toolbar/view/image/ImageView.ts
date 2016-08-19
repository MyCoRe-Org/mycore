/// <reference path="../../../../definitions/jquery.d.ts" />

module mycore.viewer.widgets.toolbar {

    export interface ImageView {
        updateHref(href: string): void; 
        getElement(): JQuery;
    }
}