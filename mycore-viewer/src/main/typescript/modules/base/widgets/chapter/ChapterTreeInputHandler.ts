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