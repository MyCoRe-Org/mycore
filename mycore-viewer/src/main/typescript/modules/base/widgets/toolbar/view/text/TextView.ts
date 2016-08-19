/// <reference path="../../../../definitions/jquery.d.ts" />

module mycore.viewer.widgets.toolbar {

    export interface TextView {
        updateText(text: string): void; 
        getElement(): JQuery;
    }
}