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

/// <reference path='../../../desktop/components/model/MyCoReDesktopToolbarModel.ts' />
namespace mycore.viewer.model {
    export class MyCoReFrameToolbarModel extends model.MyCoReDesktopToolbarModel {
        public closeToolbarButton: mycore.viewer.widgets.toolbar.ToolbarButton;

        constructor() {
            super('MyCoReFrameToolbar');
        }


        public addComponents():void {
            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);
            this.addGroup(this._imageChangeControllGroup);

            //this.addGroup(this._layoutControllGroup);
            //this.addGroup(this._actionControllGroup);
            const logoGroup = this.getGroup('LogoGroup');
            if (typeof  logoGroup !== 'undefined') {
                this.removeGroup(logoGroup);
            }

            this.closeToolbarButton = new mycore.viewer.widgets.toolbar.ToolbarButton('MaximizeButton', '', 'maximize', 'expand');
            const toolbarGroup = new mycore.viewer.widgets.toolbar.ToolbarGroup('MaximizeToolbarGroup', 100, true);

            this.addGroup(toolbarGroup);
            toolbarGroup.addComponent(this.closeToolbarButton);


        }

        public shrink(){
            this._sidebarControllGroup.removeComponent(this._sidebarControllDropdownButton);
            this.removeGroup(this._sidebarControllGroup);
            if (viewerDeviceSupportTouch) {
                this._zoomControllGroup.removeComponent(this._zoomInButton);
                this._zoomControllGroup.removeComponent(this._zoomOutButton);
                this._zoomControllGroup.removeComponent(this._rotateButton);
            }
        }


    }
}
