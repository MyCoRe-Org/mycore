/// <reference path="../../../../definitions/jquery.d.ts" />
/// <reference path="../button/ButtonView.ts" />

namespace mycore.viewer.widgets.toolbar {

    export interface DropdownView extends ButtonView {
        updateChilds(childs:Array<{id:string;label:string;icon:string
        }>): void;
        getChildElement(id:string): JQuery;
    }
}