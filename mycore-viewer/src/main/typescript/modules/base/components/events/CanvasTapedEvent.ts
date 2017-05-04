/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/StructureImage.ts" />

namespace mycore.viewer.components.events {
    export class CanvasTapedEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent) {
            super(component, CanvasTapedEvent.TYPE);
        }

        public static TYPE:string = "CanvasTapedEvent";

    }
}
