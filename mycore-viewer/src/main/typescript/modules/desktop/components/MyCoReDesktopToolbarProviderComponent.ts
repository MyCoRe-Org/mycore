/// <reference path="model/MyCoReDesktopToolbarModel.ts" />

namespace mycore.viewer.components {

    export class MyCoReDesktopToolbarProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            this.trigger(new events.ProvideToolbarModelEvent(
                this, new mycore.viewer.model.MyCoReDesktopToolbarModel()));
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReDesktopToolbarProviderComponent);