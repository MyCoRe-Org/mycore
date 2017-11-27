/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
