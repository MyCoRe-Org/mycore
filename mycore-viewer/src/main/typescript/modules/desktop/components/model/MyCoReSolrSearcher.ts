/// <reference path="../../widgets/solr/SolrSearchRequest.ts" />

namespace mycore.viewer.model {
    export class MyCoReSolrSearcher extends MyCoReViewerSearcher {

        constructor(private solrHandlerURL:string, private solrFieldName:string, private derivateId:string) {
            super();
        }

        private resolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void = null;

        public index(model:model.StructureModel, textContentResolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void, processIndicator:(x, ofY)=>void) {
            super.index(model, textContentResolver, processIndicator);
            model._imageList.forEach((image)=>this._altoHrefPageMap.set(image.additionalHrefs.get(MyCoReSolrSearcher.TEXT_HREF), image));
            processIndicator(1, 1);
            this.resolver = textContentResolver;
        }

        public static CONTEXT_SIZE = 100;
        private static TEXT_HIGHLIGHT_CLASSNAME = "matched";

        private _altoHrefPageMap = new MyCoReMap<string, model.StructureImage>();
        private _currentRequest:widgets.solr.SolrSearchRequest = null;
        private static TEXT_HREF = "AltoHref";


        public search(query:string, resultReporter:(objects:Array<ResultObject>)=>void, searchCompleteCallback:(maxResults?:number)=>void, count?:number, start?:number) {
            // first stop running request!
            if(this._currentRequest != null && !this._currentRequest.isComplete){
                this._currentRequest.abortRequest();
            }

            if(query==""){
                resultReporter((new Array()));
                return;
            }

            this._currentRequest = new widgets.solr.SolrSearchRequest(query, ()=> {
                console.log(this._currentRequest.solrRequestResult);
                resultReporter(this.extractSearchResults(query, this._currentRequest.solrRequestResult));
                searchCompleteCallback(this._currentRequest.solrRequestResult.response.numFound);
            }, this.solrHandlerURL, this.solrFieldName, this.derivateId, 10000, 0);

            this._currentRequest.startRequest();
        }

        public extractSearchResults(query:string, solrResult:widgets.solr.SolrRequestResult):Array<ResultObject> {
            var results = new Array<ResultObject>();
            solrResult.wordcoordinates.forEach((page)=> {
                var pathParts = page.id.split("/");
                pathParts.shift();
                var path = pathParts.join("/");
                var altoHref = path;
                if (!this._altoHrefPageMap.has(altoHref)) {
                    console.error("solr results contains a alto file which is not found in alto!");
                    return;
                }

                var metsPage = this._altoHrefPageMap.get(altoHref);
                page.hits.forEach((hit)=> {
                    var contextInnerHTML = hit.hl.replace("<em>", "<em class='" + MyCoReSolrSearcher.TEXT_HIGHLIGHT_CLASSNAME + "'>");
                    var payload = hit.payload[0];
                    var context = document.createElement("div");
                    context.innerHTML = contextInnerHTML;

                    var result = new ResultObject(new SolrAltoTextContent(payload, metsPage.href), query.split(" "), jQuery(context));
                    (<any>result).order = metsPage.order;
                    results.push(result);
                });
            });

            return results.sort((x, y)=>(<any>x).order - (<any>y).order);
        }
    }

    export class SolrAltoTextContent implements model.TextElement {
        constructor(positionSTR:string, parentId:string) {
            var parts = positionSTR.split("|");
            var off = 0;

            this.angle = 0;
            this.size = new Size2D(parseFloat(parts[off + 2]), parseFloat(parts[off + 3]));
            this.pos = new Position2D(parseFloat(parts[off + 0]), parseFloat(parts[off + 1]));
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
