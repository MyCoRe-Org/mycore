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

/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="IviewChapterTree.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export interface ChapterTreeInputHandler {
        // called from ctrl
        register(ctrl: IviewChapterTree): void;

        /**
        * Called if a chapter was inserted in the tree. 
        * Use this to register events. e.g. onclick.
        * 
        * node is the label of the node.
        * id is the id of the node.
        */
        registerNode(node: JQuery, id: string): void;
        
        /**
         * Called if a chapter was inserted and the chapter can hold children.
         * Use this to register events. e.g. onclick.
         * 
         * expander is the "arrow" to expand the tree.
         * id is the id of the node with has the expander.
         */
        registerExpander(expander: JQuery, id: string): void;
    }

    export class DefaultChapterTreeInputHandler implements ChapterTreeInputHandler {

        constructor() {
        }



        private _ctrl: IviewChapterTree;

        register(ctrl: IviewChapterTree): void {
            this._ctrl = ctrl;
        }

        registerNode(node: JQuery, id: string): void {
            var that = this;
            node.click(function() {
                var newSelectedChapter = that._ctrl.getChapterById(id);
                that._ctrl.setChapterSelected(newSelectedChapter);
            });
        }

        registerExpander(expander: JQuery, id: string): void {
            var that = this;
            expander.click(function() {
                var chapterToChange = that._ctrl.getChapterById(id);
                that._ctrl.setChapterExpanded(chapterToChange, !that._ctrl.getChapterExpanded(chapterToChange));
            });
        }
    }

}
