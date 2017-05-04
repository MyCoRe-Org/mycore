/// <reference path="../widgets/canvas/DoublePageLayout.ts" />
/// <reference path="../widgets/canvas/DoublePageRelocatedLayout.ts" />

namespace mycore.viewer.components {

    export class MyCoRePageDesktopLayoutProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            this.trigger(new events.ProvidePageLayoutEvent(this, new widgets.canvas.SinglePageLayout(), true));
            this.trigger(new events.ProvidePageLayoutEvent(this, new widgets.canvas.DoublePageLayout()));
            this.trigger(new events.ProvidePageLayoutEvent(this, new widgets.canvas.DoublePageRelocatedLayout()));
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoRePageDesktopLayoutProviderComponent);