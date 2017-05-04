/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class WaitForEvent extends MyCoReImageViewerEvent {
        constructor(component: ViewerComponent, public eventType:string) {
            super(component, WaitForEvent.TYPE);
        }
        
        public static TYPE = "WaitForEvent";

    }

}