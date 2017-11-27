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

namespace mycore.viewer.widgets.toolbar {

    export class MobileGroupView {

        constructor(id:string, align:string) {
            this._element = jQuery("<div></div>");
            this._element.addClass("group");
            this._element.attr("data-id", id);
            this._element.css({"float": align});
        }

        private _element:JQuery;

        public addChild(child:JQuery):void {
            this._element.append(child);
        }

        public removeChild(child:JQuery):void {
            child.remove();
        }

        public getElement():JQuery {
            return this._element;
        }
    }
}
