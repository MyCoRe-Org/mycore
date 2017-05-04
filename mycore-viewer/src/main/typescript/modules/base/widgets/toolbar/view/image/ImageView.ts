/// <reference path="../../../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface ImageView {
        updateHref(href: string): void; 
        getElement(): JQuery;
    }
}