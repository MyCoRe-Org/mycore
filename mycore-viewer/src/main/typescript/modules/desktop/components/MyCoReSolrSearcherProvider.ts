/// <reference path="model/MyCoReSolrSearcher.ts" />
/// <reference path="events/ProvideViewerSearcherEvent.ts" />

namespace mycore.viewer.components {

    export class MyCoReSolrSearcherProvider extends ViewerComponent {

        constructor(private _settings:SolrSearcherSettings) {
            super();
            this._settings.solrHandlerURL =  this._settings.webApplicationBaseURL + "/rsc/alto/highlight";
        }

        public get handlesEvents():string[] {
            return [];
        }

        public init() {
            if (this._settings.doctype == "mets") {
                this.trigger(new events.ProvideViewerSearcherEvent(
                    this, new mycore.viewer.model.MyCoReSolrSearcher(this._settings.solrHandlerURL, this._settings.derivate)));
            }
        }
    }

    export class SolrSearcherSettings extends MyCoReViewerSettings {
        solrHandlerURL:string;
    }
}


addViewerComponent(mycore.viewer.components.MyCoReSolrSearcherProvider);