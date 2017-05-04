
namespace mycore.viewer.components {

    export class MyCoRePageMobileLayoutProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            this.trigger(new events.ProvidePageLayoutEvent(this, new widgets.canvas.SinglePageLayout(), true));
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoRePageMobileLayoutProviderComponent);
