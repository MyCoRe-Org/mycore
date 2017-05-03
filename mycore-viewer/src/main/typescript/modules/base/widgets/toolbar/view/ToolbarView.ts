/// <reference path="../../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface ToolbarView {
        addChild(child: JQuery): void;
        removeChild(child: JQuery): void;
        getElement(): JQuery;
    }
}