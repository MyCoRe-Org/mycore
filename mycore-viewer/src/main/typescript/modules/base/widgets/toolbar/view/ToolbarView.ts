/// <reference path="../../../definitions/jquery.d.ts" />

module mycore.viewer.widgets.toolbar {

    export interface ToolbarView {
        addChild(child: JQuery): void;
        removeChild(child: JQuery): void;
        getElement(): JQuery;
    }
}