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

/// <reference path="../../widgets/solr/SolrSearchRequest.ts" />

namespace mycore.viewer.model {
    import HighlightPosition = mycore.viewer.widgets.solr.HighlightPosition;

    export class MyCoReSolrSearcher extends MyCoReViewerSearcher {

        constructor(private solrHandlerURL:string, private derivateId:string) {
            super();
        }

        private resolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void = null;

        public index(model:model.StructureModel, textContentResolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void, processIndicator:(x, ofY)=>void) {
            super.index(model, textContentResolver, processIndicator);
            model._imageList.forEach((image)=>this._altoHrefPageMap.set(image.additionalHrefs.get(MyCoReSolrSearcher.TEXT_HREF), image));
            processIndicator(1, 1);
            this.resolver = textContentResolver;
        }

        private static TEXT_HIGHLIGHT_CLASSNAME = "matched";

        private _altoHrefPageMap = new MyCoReMap<string, model.StructureImage>();
        private _currentRequest:widgets.solr.SolrSearchRequest = null;
        private static TEXT_HREF = "AltoHref";

        public search(query: string, resultReporter: (objects: Array<ResultObject>) => void,
                      searchCompleteCallback: () => void, count?: number, start?: number) {
            // first stop running request!
            if (this._currentRequest != null && !this._currentRequest.isComplete) {
                this._currentRequest.abortRequest();
            }

            if (query == "") {
                resultReporter(([]));
                return;
            }

            this._currentRequest = new widgets.solr.SolrSearchRequest(this.solrHandlerURL, this.derivateId, query, () => {
                console.log(this._currentRequest.solrRequestResult);
                resultReporter(this.extractSearchResults(query, this._currentRequest.solrRequestResult));
                searchCompleteCallback();
            });

            this._currentRequest.startRequest();
        }

        public extractSearchResults(query: string, solrResult: Array<widgets.solr.HighlightPage>): Array<ResultObject> {
            let results = [];
            solrResult.forEach((page) => {
                let pathParts = page.id.split("/");
                pathParts.shift();
                let altoHref = pathParts.join("/");
                if (!this._altoHrefPageMap.has(altoHref)) {
                    console.error("solr results contains a alto file which is not found in alto!");
                    return;
                }

                let metsPage = this._altoHrefPageMap.get(altoHref);
                page.hits.forEach((hit) => {
                    let contextInnerHTML = hit.hl.split("<em>")
                        .join("<em class='" + MyCoReSolrSearcher.TEXT_HIGHLIGHT_CLASSNAME + "'>");
                    let context = document.createElement("div");
                    context.innerHTML = contextInnerHTML;
                    let matchWords: Array<string> = hit.positions.map(pos => pos.content);
                    let altoTextContents = [];
                    hit.positions.forEach(position => {
                        altoTextContents.push(new SolrAltoTextContent(position, metsPage.href));
                    });
                    let result = new ResultObject(altoTextContents, matchWords, jQuery(context));
                    (<any>result).order = metsPage.order;
                    results.push(result);
                });
            });
            return results.sort((x, y) => (<any>x).order - (<any>y).order);
        }
    }

    export class SolrAltoTextContent implements model.TextElement {
        constructor(position:HighlightPosition, parentId:string) {
            this.angle = 0;
            this.size = new Size2D(position.width, position.height);
            this.pos = new Position2D(position.xpos, position.vpos);
            this.fontFamily = "arial";
            this.fontSize = this.size.height;
            this.fromBottomLeft = false;
            this.pageHref = parentId;
        }

        public angle:number;
        public size:Size2D;
        public pos:Position2D;
        public fontFamily:string;
        public fontSize:number;
        public fromBottomLeft:boolean;
        public pageHref:string;
        public text:string;

        toString() {
            return this.pageHref.toString + "-" + this.pos.toString() + "-" + this.size.toString();
        }
    }


}
