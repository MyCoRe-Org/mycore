/// <reference path="../../../Utils.ts" />
/// <reference path="ToolbarButton.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class ToolbarDropdownButton extends ToolbarButton {

        constructor(id:string, label:string, children:Array<ToolbarDropdownButtonChild>, icon:string = null, largeContent:boolean = false, buttonClass:string = "default", disabled:boolean = false, active:boolean = false) {
            super(id, label, null, icon, buttonClass, disabled, active);
            this.addProperty(new ViewerProperty<Array<ToolbarDropdownButtonChild>>(this, "children", children));
            this.addProperty(new ViewerProperty<boolean>(this, "largeContent", largeContent));
        }

        public get children():Array<ToolbarDropdownButtonChild> {
            return this.getProperty("children").value;
        }

        public set children(childs:Array<ToolbarDropdownButtonChild>) {
            this.getProperty("children").value = childs;
        }

        public get largeContent():boolean {
            return this.getProperty("largeContent").value;
        }

        public set largeContent(largeContent:boolean) {
            this.getProperty("largeContent").value = largeContent;
        }

        //public addChild(child: ToolbarDropdownButtonChild) {
        //}

    }

    export interface ToolbarDropdownButtonChild {
        id: string;
        label: string;
        isHeader?: boolean;
        icon?:string;
    }
}