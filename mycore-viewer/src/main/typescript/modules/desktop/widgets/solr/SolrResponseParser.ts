namespace mycore.viewer.widgets.solr {

    export class SolrResponseParser {
        constructor() {
        }

        public parseResponse(response:string):SolrRequestResult {
            var solrRequestResult = <SolrRequestResult>JSON.parse(response);

            if (typeof solrRequestResult.wordcoordinates == "undefined" || solrRequestResult.wordcoordinates == null) {
                solrRequestResult.wordcoordinates = new Array<SolrResultPage>();
            }

            return  solrRequestResult;
        }
    }

    export interface SolrRequestResult {
        responseHeader : SolrResponseHeader;
        response: SolrResponse;
        wordcoordinates: Array<SolrResultPage>;
    }

    export interface SolrResponseHeader {
        status:number;
        QTime:number;
    }

    export interface SolrResponse {
        numFound:number;
    }

    export interface SolrResultPage {
        id: string;
        hits: Array<SolrResultHit>;
    }

    export interface SolrResultHit {
        hl: string;
        payload: Array<string>;
    }

}