/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/StructureImage.ts" />

namespace mycore.viewer.components.events {
    export class TextEditEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, public edit:boolean = true) {
            super(component, TextEditEvent.TYPE);
        }

        public static TYPE:string = "TextEditEvent";

    }
}
