namespace mycore.viewer.model {
    export class MyCoReViewerSearcher {


         constructor() {
        }

         /*
         * @param _model the StrucutreModel
         * @param _textContentResolver function which can be used to resolve text content
         * @param _processIndicator if the indexing is a async process you can tell the user some Progress. You have to tell at least 1/1 then search is ready.
         */
        public index(model:model.StructureModel, textContentResolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void, processIndicator:(x, ofY)=>void) {
            this.model = model;
            this.textContentResolver = textContentResolver;
            this.processIndicator = processIndicator;
        }

        public model:model.StructureModel;
        public textContentResolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void;
        public processIndicator:(x, ofY)=>void;

        /**
         * Searches for the query
         * @param query the query specified by the user
         * @param resultReporter a function which can be called multiple times to report results.
         * @param searchCompleteCallback should be called if the search is completed. You can permit maxResults if you want.
         * @param count [OPTIONAL] set the maximal count of objects
         * @param start [OPTIONAL] set the start position of resultlist
         */
        public search(query:string, resultReporter:(objects:Array<ResultObject>)=>void, searchCompleteCallback:(maxResults?:number)=>void, count?:number, start?:number) {
            throw new ViewerError(this + " doesnt implements search();", { "searchMethod": this.search});
        }

    }

    export class ResultObject {
        constructor(public obj:model.TextElement, public matchWords:Array<string>, public context:JQuery) {
        }
    }
}