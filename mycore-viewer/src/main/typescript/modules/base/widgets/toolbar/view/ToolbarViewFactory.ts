/// <reference path="text/TextView.ts" />
/// <reference path="image/ImageView.ts" />
/// <reference path="group/GroupView.ts" />
/// <reference path="dropdown/DropdownView.ts" />
/// <reference path="button/ButtonView.ts" />
/// <reference path="input/TextInputView.ts" />
/// <reference path="ToolbarView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export interface ToolbarViewFactory {
        createToolbarView():ToolbarView;
        createTextView(id:string):TextView;
        createImageView(id:string):ImageView;
        createGroupView(id:string, align:string):GroupView;
        createDropdownView(id:string):DropdownView;
        createLargeDropdownView(id:string):DropdownView;
        createButtonView(id:string):ButtonView;
        createTextInputView(id: string): TextInputView;
    }

    export var ToolbarViewFactoryImpl: ToolbarViewFactory;
}



