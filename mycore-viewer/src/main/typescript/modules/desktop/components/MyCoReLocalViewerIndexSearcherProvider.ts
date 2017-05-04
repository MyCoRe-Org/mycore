/// <reference path="model/MyCoReLocalIndexSearcher.ts" />
/// <reference path="events/ProvideViewerSearcherEvent.ts" />

namespace mycore.viewer.components {

    export class MyCoReLocalViewerIndexSearcherProvider extends ViewerComponent {

        constructor(private _settings:MyCoReViewerSettings) {
            super();
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            if (this._settings.doctype == "pdf") {
                this.trigger(new events.ProvideViewerSearcherEvent(
                    this, new mycore.viewer.model.MyCoReLocalIndexSearcher()));
            }
        }
    }

}

addViewerComponent(mycore.viewer.components.MyCoReLocalViewerIndexSearcherProvider);