/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/CanvasMarker.ts" />
/// <reference path="../ViewerComponent.ts" />

module mycore.viewer.components.events {
    export class MarkerInitializedEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public marker:mycore.viewer.widgets.canvas.CanvasMarker) {
            super(component, MarkerInitializedEvent.TYPE);
        }

        public static TYPE = "MarkerInitializedEvent";

    }
}