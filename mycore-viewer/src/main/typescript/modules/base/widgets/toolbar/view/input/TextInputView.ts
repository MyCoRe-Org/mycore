/// <reference path="../../../../definitions/jquery.d.ts" />

module mycore.viewer.widgets.toolbar {

    export interface TextInputView {
        updateValue(value: string): void;
        updatePlaceholder(placeHolder:string):void;
        getValue():string;
        getElement(): JQuery;
        onChange:()=>void;
    }
}
