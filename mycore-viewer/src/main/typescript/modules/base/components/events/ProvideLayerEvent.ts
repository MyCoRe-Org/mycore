/// <reference path="MyCoReImageViewerEvent.ts" />
/// <reference path="../../widgets/canvas/PageLayout.ts" />
/// <reference path="../ViewerComponent.ts" />

namespace mycore.viewer.components.events {
    export class ProvideLayerEvent extends MyCoReImageViewerEvent {

        constructor(component:ViewerComponent, public layer:model.Layer) {
            super(component, ProvideLayerEvent.TYPE);
        }

        public static TYPE = "ProvideLayerEvent";

    }
}