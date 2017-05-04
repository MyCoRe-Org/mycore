/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class RedrawEvent extends MyCoReImageViewerEvent {
        constructor(component: ViewerComponent) {
            super(component, RedrawEvent.TYPE);
        }

        public static TYPE:string = "RedrawEvent";

    }
}
