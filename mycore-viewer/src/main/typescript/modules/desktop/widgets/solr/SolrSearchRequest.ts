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

namespace mycore.viewer.widgets.solr {
    export class SolrSearchRequest {

        constructor(private solrHandlerURL:string,
                    private derivateID:string,
                    public query:string,
                    public requestCallback:(success:boolean) => void) {
        }

        public static BASE_TEMPLATE = "{solrHandlerURL}/{derivateID}?q={query}";

        private request:JQueryXHR = null;

        private _solrRequestResult:Array<HighlightPage> = null;
        private _isComplete:boolean = false;

        public get solrRequestResult() {
            return this._solrRequestResult;
        }

        public get isComplete() {
            return this._isComplete;
        }

        public startRequest() {
            let requestURL = this.buildRequestURL();
            let ajaxSettings = {url: requestURL,
                async: true,
                success: (response) => {
                    if (!this.isComplete) {
                        this.processResponse(response);
                        this.requestCallback(true);
                    }
                },
                error: (request, status, exception) => {
                    console.log(exception);
                    this.requestCallback(false);
                }
            };
            this.request = jQuery.ajax(<JQueryAjaxSettings>ajaxSettings);
        }

        public abortRequest() {
            if (this.isComplete) {
                console.debug("request is already complete!");
                return;
            }

            if (this.request == null || typeof this.request == "undefined") {
                console.debug("request object is null!");
                return;
            }

            this.requestCallback = ()=> {
            };
            this.request.abort("request abort");
        }

        private processResponse(response:Array<HighlightPage>) {
            this._isComplete = true;
            this._solrRequestResult = response;
        }

        private buildRequestURL() {
            return ViewerFormatString(SolrSearchRequest.BASE_TEMPLATE, {
                solrHandlerURL: this.solrHandlerURL,
                query: this.query,
                derivateID: this.derivateID
            });
        }
    }

    export interface HighlightPage {
        id: string;
        hits: Array<HighlightHit>;
    }

    export interface HighlightHit {
        hl: string;
        positions: Array<HighlightPosition>;
    }

    export interface HighlightPosition {
        content: string;
        xpos: number;
        vpos: number;
        width: number;
        height: number;
    }

}
