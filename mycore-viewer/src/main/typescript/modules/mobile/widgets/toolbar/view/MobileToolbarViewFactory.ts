/// <reference path="group/MobileGroupView.ts" />
/// <reference path="dropdown/MobileDropdownView.ts" />
/// <reference path="button/MobileButtonView.ts" />
/// <reference path="MobileToolbarView.ts" />

module mycore.viewer.widgets.toolbar {
    export class MobileToolbarViewFactory implements ToolbarViewFactory {
        createToolbarView():ToolbarView {
            return new MobileToolbarView();
        }

        createTextView(id:string):TextView {
            throw new ViewerError("text view not supported by Mobile!");
        }

        createImageView(id:string):ImageView {
            throw new ViewerError("image view not supported by Mobile!");
        }

        createGroupView(id:string, align:string):GroupView {
            return new MobileGroupView(id, align);
        }

        createDropdownView(id:string):DropdownView {
            return new MobileDropdownView(id);
        }

        createLargeDropdownView(id:string):DropdownView {
            return new MobileDropdownView(id);
        }

        createButtonView(id:string):ButtonView {
            return new MobileButtonView(id);
        }
    }

    ToolbarViewFactoryImpl = new MobileToolbarViewFactory();
}



