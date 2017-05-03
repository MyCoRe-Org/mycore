/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/PageLayout.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    /**
     * Fired when a PageLayoutChanges
     */
    export class PageLayoutChangedEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public pageLayout:widgets.canvas.PageLayout) {
            super(component, PageLayoutChangedEvent.TYPE);
        }

        public static TYPE = "PageLayoutChangedEvent";

    }
}