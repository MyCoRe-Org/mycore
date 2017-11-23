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

namespace mycore.viewer.widgets.index {
    export class TextIndex<T> {

        constructor(private _fullTextProvider:(T) => string) {

        }

        // x chars before and after the found elem
        private static DEFAULT_CONTEXT_SIZE = 100;
        private static TEXT_HIGHLIGHT_CLASSNAME = "matched";

        private static MATCH_TEMPLATE_WORD = "(\\b([a-zA-Z]*%TOKEN%[a-zA-Z]*)\\b)";
        private static MATCH_TEMPLATE_WORD_SHORT = "(\\b%TOKEN%\\b)";
        private static MATCH_TEMPLATE = "%WORDS%";
        private static MATCH_PARAMETER = "gim";
        private static MATCH_WORD_REGEXP = new RegExp("\\b([a-zA-Z]+)\\b", TextIndex.MATCH_PARAMETER);


        private _fullTextInformationIndex = [];
        private _currentPosition = 0;
        private _fullText:string = "";

        public addElement(elem:T) {
            let fullTextPart = this._fullTextProvider(elem);

            TextIndex.MATCH_WORD_REGEXP.lastIndex = 0;
            let match;
            while ((match = TextIndex.MATCH_WORD_REGEXP.exec(fullTextPart)) != null) {
                this._fullTextInformationIndex[this._currentPosition + match.index] = elem;
            }

            if (fullTextPart.charAt(fullTextPart.length - 1) == " ") {
                this._fullText += fullTextPart;
                this._currentPosition += fullTextPart.length;
            } else {
                this._fullText += fullTextPart + " ";
                this._currentPosition += fullTextPart.length + 1;
            }

        }

        public search(searchInput:string):SearchResult<T> {

            let regExpSearch = "";

            let searchWords = searchInput.split(/\s/).filter((w)=>w != "");
            searchWords.forEach((expr, i)=> {
                regExpSearch += ((i == 0) ? "" : "\\s") + ((searchInput.length<=3) ? TextIndex.MATCH_TEMPLATE_WORD_SHORT.replace("%TOKEN%", expr) : TextIndex.MATCH_TEMPLATE_WORD.replace("%TOKEN%", expr));
            });


            let searchRegExp = new RegExp(TextIndex.MATCH_TEMPLATE.replace("%WORDS%", regExpSearch), TextIndex.MATCH_PARAMETER);
            let resultObjects = new Array<IndexResultObject<T>>();
            let match;
            let limit = 1000;

            if(searchInput.length > 0 || searchInput.replace(" ","").length > 0) {
                while ((match = searchRegExp.exec(this._fullText)) && limit--) {
                    this.extractResults(match, resultObjects, searchWords);
                }
            }

            return new SearchResult<T>(resultObjects, resultObjects.length);
        }

        private extractResults(match, resultObjects, searchWords) {
            let si = match.index;
            let result = match[0];
            let words = result.split(" ");
            let wordLen = 0;
            let index = -1;
            let context = this.getContext(si + result.length - (result.length / 2), searchWords);
            let textline = null;
            do {
                if (typeof this._fullTextInformationIndex[si + wordLen] != "undefined") {
                    textline = this._fullTextInformationIndex[si + wordLen];
                    resultObjects.push(new IndexResultObject<T>([textline], searchWords, context));
                }
                index++;
                wordLen += words[index].length + 1;
            } while (index + 1 < words.length);

            return textline;
        }

        public getContext(pos:number, words:Array<string>) {
            let html = this._fullText.substr(Math.max(0, pos - TextIndex.DEFAULT_CONTEXT_SIZE / 2), TextIndex.DEFAULT_CONTEXT_SIZE);
            words = words.sort((w1, w2) => w1.length - w2.length);
            words.forEach((w)=> {
                if (w != "") {
                    html = html.replace(new RegExp(w, "gim"), "<span class='" + TextIndex.TEXT_HIGHLIGHT_CLASSNAME + "'>$&</span>")
                }
            });
            return jQuery("<span>" + html + "</span>");
        }

    }

    export class SearchResult<T> {
        constructor(public results:Array<IndexResultObject<T>>, public count:number) {
        }
    }

    export class IndexResultObject<T> {
        constructor(public arr:Array<T>, public matchWords:Array<string>, public context:JQuery) {
        }
    }

    export interface Tokenizer<T> {
        getToken(elem:T): Array<string>;
    }
}
