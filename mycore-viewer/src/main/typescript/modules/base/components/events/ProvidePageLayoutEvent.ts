/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/PageLayout.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    /**
     * Used to provide a PageLayout wich can be used by the MyCoReImageScrollComponent.
     */
    export class ProvidePageLayoutEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public pageLayout:widgets.canvas.PageLayout, public isDefault=false) {
            super(component, ProvidePageLayoutEvent.TYPE);
        }

        public static TYPE = "ProvidePageLayoutEvent";

    }
}