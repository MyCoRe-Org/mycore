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
    export class MyCoReMobileToolbarModel extends MyCoReBasicToolbarModel {
        constructor() {
            super("MyCoReMobileToolbar");
        }


        public addComponents():void {


            this.addGroup(this._sidebarControllGroup);
            this.addGroup(this._zoomControllGroup);

            if (viewerDeviceSupportTouch) {
                this._zoomControllGroup.removeComponent(this._zoomInButton);
                this._zoomControllGroup.removeComponent(this._zoomOutButton);
                this._zoomControllGroup.removeComponent(this._rotateButton);            }

            this.addGroup(this._actionControllGroup);
            this.addGroup(this._closeViewerGroup);

            this.changeIcons();
        }

        public changeIcons():void {
            this._zoomInButton.icon = "search-plus";
            this._zoomOutButton.icon = "search-minus";
            this._zoomFitButton.icon = "expand";
            this._zoomWidthButton.icon = "arrows-h";
            this._closeViewerButton.icon = "power-off";
            this._rotateButton.icon = "rotate-right";
        }

    }
}
