/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../model/AbstractPage.ts" />

namespace mycore.viewer.components.events {
    export class PageLoadedEvent extends MyCoReImageViewerEvent {
        constructor(component: ViewerComponent, public _pageId: string, public abstractPage: model.AbstractPage) {
            super(component, PageLoadedEvent.TYPE);
        }

        public static TYPE: string = "PageLoadedEvent";

    }
}
