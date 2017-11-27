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
            let resolver = this.textContentResolver;
            let processIndicator = this.processIndicator;
            let count = 0;
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
            let contextExists = {};
            return searchResults.filter((el) => {
                let key = (Utils.hash(el.context.text() + el.matchWords.join("")));
                if (key in contextExists) {
                    return false;
                } else {
                    contextExists[key] = true;
                    return true;
                }
            });
        }

        public search(query:string, resultReporter:(objects:Array<ResultObject>)=>void, searchCompleteCallback:(maxResults?:number)=>void, count?:number, start?:number) {
            let results = this._searchIndex.search(query).results;
            results = this.clearDoubleResults(results);
            resultReporter(results);
            searchCompleteCallback(results.length);
        }

    }

}
