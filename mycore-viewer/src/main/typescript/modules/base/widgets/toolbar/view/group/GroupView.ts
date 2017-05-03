/// <reference path="../../../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface GroupView {
        addChild(child: JQuery): void;
        removeChild(child: JQuery): void;
        getElement(): JQuery;
    }
}