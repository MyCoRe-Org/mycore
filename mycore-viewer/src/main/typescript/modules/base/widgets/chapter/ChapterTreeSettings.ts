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