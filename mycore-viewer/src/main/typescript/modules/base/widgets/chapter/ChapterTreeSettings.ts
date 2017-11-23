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
/// <reference path="ChapterTreeChapter.ts" />
/// <reference path="ChapterTreeView.ts" />
/// <reference path="DesktopChapterTreeView.ts" />
/// <reference path="MobileChapterTreeView.ts" />
/// <reference path="ChapterTreeInputHandler.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export interface ChapterTreeSettings {
        /**
         * Should hold the Chapters to Display.
         */
        chapter: ChapterTreeChapter;

        /**
         * holds wich image is referenced by wich chapter
         */
        chapterLabelMap: MyCoReMap<string, string>;

        /**
         * Should hold the DOM-Node were the Chapter Tree should be inserted
         */
        container: JQuery;

        /**
         * Should hold a ChapterTreeViewFactory wich creates the View for the ChapterTree
         */
        viewFactory: ChapterTreeViewFactory;

        inputHandler: ChapterTreeInputHandler;
    }

    /**
     * The IviewChapterTree doesnt know how to build the View.
     * So you can Exchange the view without change the Controller.
     */
    export interface ChapterTreeViewFactory {
        createChapterTeeView(): ChapterTreeView;
    }


    /**
     * Default implementation of the ChapterTreeSettings
     */
    export class DefaultChapterTreeSettings implements ChapterTreeSettings, ChapterTreeViewFactory {

        constructor(private _container: JQuery, private _chapterLabelMap: MyCoReMap<string, string>, private _chapter: ChapterTreeChapter = null, private mobile: boolean = false, private _inputHandler: ChapterTreeInputHandler = new DefaultChapterTreeInputHandler()) {
        }


        public get chapter() {
            return this._chapter;
        }

        public get chapterLabelMap() {
            return this._chapterLabelMap;
        }

        public get container() {
            return this._container;
        }

        public get viewFactory() {
            return this;
        }

        public get inputHandler(): ChapterTreeInputHandler {
            return this._inputHandler;
        }

        createChapterTeeView(): ChapterTreeView {
            return (this.mobile) ? new MobileChapterTreeView(this.container, this.inputHandler) : new DesktopChapterTreeView(this.container, this.inputHandler);
        }

    }
}
