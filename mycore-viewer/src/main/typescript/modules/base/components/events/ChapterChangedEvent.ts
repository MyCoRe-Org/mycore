/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../../widgets/events/ViewerEvent.ts" />
/// <reference path="../model/StructureImage.ts" />

namespace mycore.viewer.components.events {
    export class ChapterChangedEvent extends MyCoReImageViewerEvent{
        constructor(component: ViewerComponent,private _chapter:model.StructureChapter) {
            super(component, ChapterChangedEvent.TYPE);
        }

        public get chapter() {
            return this._chapter;
        }

        public static TYPE:string = "ChapterChangedEvent";
    }
}