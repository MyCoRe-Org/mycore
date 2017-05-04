/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/StructureImage.ts" />
/// <reference path="../model/AbstractPage.ts" />

namespace mycore.viewer.components.events {
    export class RequestPageEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, public _pageId:string, public _onResolve:(pageId:string, abstractPage:model.AbstractPage)=> void, public textContentURL:string = null) {
            super(component, RequestPageEvent.TYPE);
        }

        public static TYPE:string = "RequestPageEvent";

    }
}