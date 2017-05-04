/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../model/MyCoReBasicToolbarModel.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class ProvideToolbarModelEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public model:model.MyCoReBasicToolbarModel) {
            super(component, ProvideToolbarModelEvent.TYPE);
        }

        public static TYPE = "ProvideToolbarModelEvent";

    }
}