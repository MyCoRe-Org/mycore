/// <reference path="../model/MyCoReViewerSearcher.ts" />

namespace mycore.viewer.components.events {
    export class ProvideViewerSearcherEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, private _searcher:model.MyCoReViewerSearcher) {
            super(component, ProvideViewerSearcherEvent.TYPE);
        }

        public get searcher(){
            return this._searcher;
        }

        public static TYPE:string = "ProvideViewerSearcherEvent";

    }
}