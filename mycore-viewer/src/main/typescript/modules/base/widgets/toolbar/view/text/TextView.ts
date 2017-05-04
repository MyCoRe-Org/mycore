/// <reference path="../../../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface TextView {
        updateText(text: string): void; 
        getElement(): JQuery;
    }
}