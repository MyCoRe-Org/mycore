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