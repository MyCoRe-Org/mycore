/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {

    export class UpdateURLEvent extends MyCoReImageViewerEvent {

        constructor(component: ViewerComponent) {
            super(component, UpdateURLEvent.TYPE);
        }

        public static TYPE:string = "UpdateURLEvent";

    }

}
