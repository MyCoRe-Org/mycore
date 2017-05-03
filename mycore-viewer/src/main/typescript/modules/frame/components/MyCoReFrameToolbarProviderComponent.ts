/// <reference path="model/MyCoReFrameToolbarModel.ts" />

namespace mycore.viewer.components {

    export class MyCoReFrameToolbarProviderComponent extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE];
        }

        public init() {
            var frameToolbarModel = new mycore.viewer.model.MyCoReFrameToolbarModel();

            this.trigger(new events.ProvideToolbarModelEvent(
                this, frameToolbarModel));
        }

        public handle(e:mycore.viewer.widgets.events.ViewerEvent):void {
            if (e.type == mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var bpe = <mycore.viewer.widgets.toolbar.events.ButtonPressedEvent>e;
                if (bpe.button.id == "MaximizeButton") {
                    this.trigger(new events.RequestPermalinkEvent(this, (permalink)=> {
                        window.top.location.assign(permalink);
                    }));
                }
            }
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReFrameToolbarProviderComponent);