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

/// <reference path="../../../desktop/components/model/MyCoReDesktopToolbarModel.ts" />
namespace mycore.viewer.model {
    export class MyCoReFrameToolbarModel extends model.MyCoReDesktopToolbarModel {
        constructor() {
            super("MyCoReFrameToolbar");
        }


        public addComponents():void {
            this._viewSelectGroup=new widgets.toolbar.ToolbarGroup("viewSelectGroup");

            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);
            this.addGroup(this._imageChangeControllGroup);

            //this.addGroup(this._layoutControllGroup);
            //this.addGroup(this._actionControllGroup);
            var logoGroup = this.getGroup("LogoGroup");
            if (typeof  logoGroup != "undefined") {
                this.removeGroup(logoGroup);
            }

            var toolbarButton = new mycore.viewer.widgets.toolbar.ToolbarButton("MaximizeButton", "", "", "fullscreen");
            var toolbarGroup = new mycore.viewer.widgets.toolbar.ToolbarGroup("MaximizeToolbarGroup", true);

            this.addGroup(toolbarGroup);
            toolbarGroup.addComponent(toolbarButton);


        }


    }
}
