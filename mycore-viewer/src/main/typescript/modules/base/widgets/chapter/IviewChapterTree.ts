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

/// <reference path="ChapterTreeModel.ts" />
/// <reference path="ChapterTreeView.ts" />
/// <reference path="ChapterTreeSettings.ts" />
/// <reference path="ChapterTreeInputHandler.ts" />
/// <reference path="ChapterTreeChapter.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export class IviewChapterTree {

        constructor(private _settings: ChapterTreeSettings) {
            this._model = new ChapterTreeModel(_settings.chapter, this._settings.chapterLabelMap);
            this._view = _settings.viewFactory.createChapterTeeView();
            this._settings.inputHandler.register(this);
            this.init();
        }

        private _model: ChapterTreeModel;
        private _view: ChapterTreeView;

        public setChapterSelected(element: ChapterTreeChapter) {
            if (this._model.selected != null) {
                this._view.setSelected(this._model.selected.id, false);
            }
            if(element == null) {
                return;
            }
            this._model.selected = element;
            this._view.setSelected(this._model.selected.id, true);
        }

        public getSelectedChapter() {
            return this._model.selected;
        }

        public setChapterExpanded(element: ChapterTreeChapter, expanded: boolean) {
            if(element!=null){
                this._model.chapterVisible.set(element.id, expanded);
                this._view.setOpened(element.id, expanded);
            }
        }

        public getChapterExpanded(element: ChapterTreeChapter) {
            return this._model.chapterVisible.get(element.id);
        }

        public getChapterById(id: string) {
            return this._model.idElementMap.get(id);
        }

        private init() {
            this.insertChapterView(this._model.root);
        }

        public jumpToChapter(chapter: ChapterTreeChapter) {
            if(chapter == null) {
                return;
            }
            this._view.jumpTo(chapter.id);
        }

        private insertChapterView(chapter: ChapterTreeChapter) {
            this._view.addNode((chapter.parent || (<any>{ id: null })).id, chapter.id, chapter.label, this._model.chapterLabelMap.get(chapter.id) || "", chapter.chapter.length > 0);
            for (var i in chapter.chapter) {
                var currentChapter = chapter.chapter[i];
                this.insertChapterView(currentChapter);
            }
        }


    }

}
