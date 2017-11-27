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

/// <reference path="../../Utils.ts" />
/// <reference path="ChapterTreeChapter.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export class ChapterTreeModel {

        constructor(public root: ChapterTreeChapter, public chapterLabelMap: MyCoReMap<string, string>) {
            this.chapterVisible = new MyCoReMap<string, boolean>();
            this.idElementMap = new MyCoReMap<string, ChapterTreeChapter>();
            this.initChapter(this.root);
            this.selected = null;
        }

        private initChapter(chapter: ChapterTreeChapter): void {
            this.idElementMap.set(chapter.id, chapter);
            this.chapterVisible.set(chapter.id, true);
            for (var index in chapter.chapter) {
                var current = chapter.chapter[index];
                this.initChapter(current);
            }
        }

        public chapterVisible: MyCoReMap<string, boolean>;
        public idElementMap: MyCoReMap<string, ChapterTreeChapter>;
        public selected: ChapterTreeChapter;
    }
}
