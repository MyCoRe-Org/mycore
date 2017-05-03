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
