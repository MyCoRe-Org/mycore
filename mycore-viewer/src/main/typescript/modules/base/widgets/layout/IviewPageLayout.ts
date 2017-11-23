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

/// <reference path="../../Utils.ts" />
/// <reference path="../../definitions/jquery.d.ts" />

namespace mycore.viewer.widgets.layout {
    export class IviewPageLayout {

        constructor(_currentPage:JQuery) {
            this._initContainer();
            this.changePage(_currentPage);

        }

        private _initContainer() {
            this._container1 = jQuery("<div></div>");
            var cssObject = <any>{};
            cssObject.position = "fixed";
            cssObject.left = "0px";
            cssObject.top = "0px";
            cssObject.bottom = "0px";
            cssObject.right = "0px";
            this._container1.css(cssObject);
            jQuery(document.body).append(this._container1);

            this._container2 = jQuery("<div></div>");
            cssObject.position = "fixed";
            cssObject.left = "0px";
            cssObject.top = "0px";
            cssObject.bottom = "0px";
            cssObject.right = "0px";
            this._container2.css(cssObject);
            jQuery(document.body).append(this._container2);
        }

        public changePage(newPage:JQuery) {
            this._container1.stop(true,true);
            this._container2.stop(true,true);

            this._container2.children().detach();
            this._container2.append(newPage);
            this._container1.fadeOut("slow");
            this._container2.fadeIn("slow");
            var b = this._container1;
            this._container1 = this._container2;
            this._container2 = b;
        }


        public static DIRECTION_EAST = 1;
        public static DIRECTION_SOUTH = 2;
        public static DIRECTION_WEST = 3;
        public static DIRECTION_NORTH = 4;

        private _container1:JQuery;
        private _container2:JQuery;

    }


}
