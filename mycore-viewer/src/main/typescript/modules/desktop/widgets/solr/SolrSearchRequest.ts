/// <reference path="SolrResponseParser.ts" />
namespace mycore.viewer.widgets.solr {
    export class SolrSearchRequest {

        constructor(public query:string,
                    public requestCallback:(success:boolean)=>void,
                    private solrHandlerURL:string,
                    private solrFieldName:string,
                    private derivateID:string,
                    public count?:number,
                    public start?:number) {
        }

        public static BASE_TEMPLATE = "{solrHandlerURL}?q={solrQuery}&wt=json&indent=true&fq=derivateID:{derivateID}";
        public static COUNT_TEMPLATE = "&rows={count}&start={start}";

        private solrResponseParser = new widgets.solr.SolrResponseParser();
        private request:JQueryXHR = null;

        private _solrRequestResult:SolrRequestResult = null;
        private _isComplete:boolean = false;

        public get solrRequestResult() {
            return this._solrRequestResult;
        }

        public get isComplete() {
            return this._isComplete;
        }

        public startRequest() {
            var requestURL = this.buildRequestURL(this.query, this.count, this.start);
            var ajaxSettings = {url: requestURL,
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

        private processResponse(response:any) {
            var resp = this.solrResponseParser.parseResponse(response);
            this._solrRequestResult = resp;
            this._isComplete = true;
        }

        private buildSolrQuery(userInput:string) {
            var query:string = "";
            var wordArray = userInput.split(" ");
            var lastPos = -1;
            while((lastPos = wordArray.indexOf(" ")) != -1){
                wordArray = wordArray.splice(lastPos);
            }
            wordArray.forEach((part:string, i)=> {
                query += this.solrFieldName + ":" + part + (wordArray.length != (i + 1) ? " AND " : "");
            });
            return (query == "") ? this.solrFieldName + ":*" : query;
        }


        private buildRequestURL(userSearchQuery:string, count?:number, start?:number) {
            var queryURL = ViewerFormatString(SolrSearchRequest.BASE_TEMPLATE, {
                solrHandlerURL: this.solrHandlerURL,
                solrQuery: this.buildSolrQuery(userSearchQuery),
                derivateID: this.derivateID
            });

            if (count != null && typeof count != "undefined" && start != null && typeof start != "undefined") {
                queryURL += ViewerFormatString(SolrSearchRequest.COUNT_TEMPLATE, {
                    count: count,
                    start: start
                });
            }

            return queryURL;
        }
    }
}