/// <reference path="../../Utils.ts" />
/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../ViewerComponent.ts" />
/// <reference path="../model/StructureModel.ts" />

namespace mycore.viewer.components.events {
    export class StructureModelLoadedEvent extends MyCoReImageViewerEvent {
        constructor(component:ViewerComponent, private _structureModel:model.StructureModel) {
            super(component, StructureModelLoadedEvent.TYPE);
        }

        public get structureModel() {
            return this._structureModel;
        }

        public static TYPE:string = "StructureModelLoadedEvent";


    }

}