/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/viewport/Viewport.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class ViewportInitializedEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public viewport:mycore.viewer.widgets.canvas.Viewport) {
            super(component, ViewportInitializedEvent.TYPE);
        }

        public static TYPE = "ViewportInitializedEvent";

    }
}