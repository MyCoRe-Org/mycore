/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class RequestPermalinkEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public callback:(permalink:string) => void) {
            super(component, RequestPermalinkEvent.TYPE);
        }

        public static TYPE:string = "RequestPermalinkEvent";

    }
}