/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/StructureImage.ts" />

namespace mycore.viewer.components.events {
    export class ImageSelectedEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, private _image:model.StructureImage) {
            super(component, ImageSelectedEvent.TYPE);
        }

        public get image() {
            return this._image;
        }

        public static TYPE:string = "ImageSelectedEvent";

    }
}