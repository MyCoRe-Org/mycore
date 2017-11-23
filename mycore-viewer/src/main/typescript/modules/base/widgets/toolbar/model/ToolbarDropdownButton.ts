/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
