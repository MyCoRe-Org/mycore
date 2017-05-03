/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/TextContent.ts" />

namespace mycore.viewer.components.events {
    export class RequestTextContentEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, public _href:string, public _onResolve:(href:string, abstractPage:model.TextContentModel)=> void) {
            super(component, RequestTextContentEvent.TYPE);
        }

        public static TYPE:string = "RequestTextContentEvent";

    }
}