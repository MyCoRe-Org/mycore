/// <reference path="../../widgets/search/TextIndex.ts" />

namespace mycore.viewer.model {
    export class MyCoReLocalIndexSearcher extends MyCoReViewerSearcher {

        constructor() {
            super();
        }

        public index(model:model.StructureModel, textContentResolver:(id:string, callback:(id:string, textContent:model.TextContentModel)=> void)=> void, processIndicator:(x, ofY)=>void) {
            super.index(model,textContentResolver,processIndicator);
            this.indexModel();
        }

        private _searchIndex = new widgets.index.TextIndex<model.TextElement>((te:model.TextElement)=>te.text);
        private static PDF_TEXT_HREF = "pdfText";

        private indexModel() {
            var resolver = this.textContentResolver;
            var processIndicator = this.processIndicator;
            var count = 0;
            this.model.imageList.forEach((image, i)=> {
                resolver(image.additionalHrefs.get(MyCoReLocalIndexSearcher.PDF_TEXT_HREF), (href, textContent)=> {
                    count++;
                    this.indexPage(textContent);
                    processIndicator(count, this.model._imageList.length-1);
                });
            });
        }

        private indexPage(text:model.TextContentModel) {
            text.content.forEach((e)=> {
                this._searchIndex.addElement(e);
            });
        }

        private clearDoubleResults(searchResults) {
            var contextExists = {};
            return searchResults.filter((el) => {
                var key = (Utils.hash(el.context.text() + el.matchWords.join("")));
                if (key in contextExists) {
                    return false;
                } else {
                    contextExists[key] = true;
                    return true;
                }
            });
        }

        public search(query:string, resultReporter:(objects:Array<ResultObject>)=>void, searchCompleteCallback:(maxResults?:number)=>void, count?:number, start?:number) {
            var results = this._searchIndex.search(query).results;
            results = this.clearDoubleResults(results);
            resultReporter(results);
            searchCompleteCallback(results.length);
        }

    }

}
