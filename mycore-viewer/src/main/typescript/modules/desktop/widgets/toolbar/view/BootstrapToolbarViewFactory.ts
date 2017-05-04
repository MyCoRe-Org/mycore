/// <reference path="text/BootstrapTextView.ts" />
/// <reference path="image/BootstrapImageView.ts" />
/// <reference path="group/BootstrapGroupView.ts" />
/// <reference path="dropdown/BootstrapDropdownView.ts" />
/// <reference path="dropdown/BootstrapLargeDropdownView.ts" />
/// <reference path="button/BootstrapButtonView.ts" />
/// <reference path="input/BootstrapTextInputView.ts" />
/// <reference path="BootstrapToolbarView.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class BootstrapToolbarViewFactory implements ToolbarViewFactory {

        createToolbarView():ToolbarView {
            return new BootstrapToolbarView();
        }

        createTextView(id:string):TextView {
            return new BootstrapTextView(id);
        }

        createImageView(id:string):ImageView {
            return new BootstrapImageView(id);
        }

        createGroupView(id:string, align:string):GroupView {
            return new BootstrapGroupView(id, align);
        }

        createDropdownView(id:string):DropdownView {
            return new BootstrapDropdownView(id);
        }

        createLargeDropdownView(id:string):DropdownView {
            return new BootstrapLargeDropdownView(id);
        }

        createButtonView(id:string):ButtonView {
            return new BootstrapButtonView(id);
        }

        createTextInputView(id: string): TextInputView {
            return new BootstrapTextInputView(id);
        }
    }

    ToolbarViewFactoryImpl = new BootstrapToolbarViewFactory();
}



