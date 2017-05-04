/// <reference path="model/MyCoReMobileToolbarModel.ts" />

namespace mycore.viewer.components {

    export class MyCoReMobileToolbarProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            this.trigger(new events.ProvideToolbarModelEvent(
                this, new mycore.viewer.model.MyCoReMobileToolbarModel()));
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReMobileToolbarProviderComponent);