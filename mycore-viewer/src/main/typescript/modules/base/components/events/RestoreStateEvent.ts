/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../../Utils.ts" />

namespace mycore.viewer.components.events {
    export class RestoreStateEvent extends MyCoReImageViewerEvent {
        constructor(component: ViewerComponent,public restoredState:MyCoReMap<string,string>) {
            super(component, RestoreStateEvent.TYPE);
        }

        public static TYPE:string = "RestoreStateEvent";

    }
}