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

namespace mycore.viewer.model {
    import DropdownButtonController = mycore.viewer.widgets.toolbar.DropdownButtonController;
    export class MyCoReDesktopToolbarModel extends model.MyCoReBasicToolbarModel {

        constructor(name = "MyCoReDesktopToolbar") {
            super(name);
        }

        public _viewSelectGroup:widgets.toolbar.ToolbarGroup;
        public viewSelectChilds:Array<widgets.toolbar.ToolbarDropdownButtonChild>;
        public viewSelect:widgets.toolbar.ToolbarDropdownButton;

        public selectionSwitchButton:widgets.toolbar.ToolbarButton;

        public addComponents():void {
            this._viewSelectGroup=new widgets.toolbar.ToolbarGroup("viewSelectGroup");

            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);
            this.addGroup(this._layoutControllGroup);
            this.addGroup(this._viewSelectGroup);
            this.addGroup(this._imageChangeControllGroup);
            this.addGroup(this._actionControllGroup);
            this.addGroup(this._closeViewerGroup);
            this.addGroup(this._searchGroup);
        }


        public addViewSelectButton():void {
            this.viewSelectChilds = new Array<widgets.toolbar.ToolbarDropdownButtonChild>();

            this.viewSelectChilds.push({
                id: "imageView",
                label: "imageView"
            });

            this.viewSelectChilds.push({
                id: "mixedView",
                label: "mixedView"
            });

            this.viewSelectChilds.push({
                id: "textView",
                label: "textView"
            });

            this.viewSelect = new widgets.toolbar.ToolbarDropdownButton("viewSelect", "viewSelect", this.viewSelectChilds, "eye-open");
            this._viewSelectGroup.addComponent(this.viewSelect);
        }

        public addSelectionSwitchButton():void{
            this.selectionSwitchButton = new widgets.toolbar.ToolbarButton("selectionSwitchButton", "", "");
            this.selectionSwitchButton.icon = "text-width";
            //this._actionControllGroup.addComponent(this.selectionSwitchButton);
        }

    }
}
