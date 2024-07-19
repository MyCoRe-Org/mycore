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


import {MyCoReViewerSearcher, ResultObject} from "./MyCoReViewerSearcher";
import {TextContentModel, TextElement} from "../../../base/components/model/TextContent";
import {StructureModel} from "../../../base/components/model/StructureModel";
import {MyCoReMap, Position2D, Size2D} from "../../../base/Utils";
import {StructureImage} from "../../../base/components/model/StructureImage";
import {HighlightPage, HighlightPosition, SolrSearchRequest} from "../../widgets/solr/SolrSearchRequest";


export class MyCoReSolrSearcher extends MyCoReViewerSearcher {

    constructor(private solrHandlerURL: string, private derivateId: string) {
        super();
    }

    private resolver: (id: string, callback: (id: string, textContent: TextContentModel) => void) => void = null;

    public index(model: StructureModel,
                 textContentResolver: (id: string,
                                       callback: (id: string, textContent: TextContentModel) => void) => void,
                 processIndicator: (x, ofY) => void) {
        super.index(model, textContentResolver, processIndicator);
        model._imageList.forEach((image) => {
            const href = image.additionalHrefs.get(MyCoReSolrSearcher.TEXT_HREF);
            if (href != null) {
                this._altoHrefPageMap.set(href, image);
            }
        });
        processIndicator(1, 1);
        this.resolver = textContentResolver;
    }

    private static TEXT_HIGHLIGHT_CLASSNAME: string = 'matched';

    private _altoHrefPageMap = new MyCoReMap<string, StructureImage>();
    private _currentRequest: SolrSearchRequest = null;
    private static TEXT_HREF: string = 'AltoHref';

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

        this._currentRequest = new SolrSearchRequest(this.solrHandlerURL, this.derivateId, query, () => {
            console.log(this._currentRequest.solrRequestResult);
            resultReporter(this.extractSearchResults(query, this._currentRequest.solrRequestResult));
            searchCompleteCallback();
        });

        this._currentRequest.startRequest();
    }

    public extractSearchResults(query: string, solrResult: Array<HighlightPage>): Array<ResultObject> {
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
                let contextInnerHTML = hit.hl.split('<em>')
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

export class SolrAltoTextContent implements TextElement {
    constructor(position: HighlightPosition, parentId: string) {
        this.angle = 0;
        this.size = new Size2D(position.width, position.height);
        this.pos = new Position2D(position.xpos, position.vpos);
        this.fontFamily = "arial";
        this.fontSize = this.size.height;
        this.fromBottomLeft = false;
        this.pageHref = parentId;
    }

    public angle: number;
    public size: Size2D;
    public pos: Position2D;
    public fontFamily: string;
    public fontSize: number;
    public fromBottomLeft: boolean;
    public pageHref: string;
    public text: string;

    toString() {
        return this.pageHref.toString + '-' + this.pos.toString() + '-' + this.size.toString();
    }
}



