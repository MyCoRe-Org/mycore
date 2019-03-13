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
    export class BootstrapToolbarView implements ToolbarView {

        constructor() {
            this._toolbar = jQuery("<nav></nav>");
            this._toolbar.addClass("navbar navbar-expand-lg navbar-light bg-light");
        }

        private _toolbar: JQuery;
//navbar-header
        public addChild(child: JQuery): void {
            this._toolbar.append(child);
            this._toolbar.find(".btn-group.right.ml-auto").removeClass("ml-auto");
            const sortedElements = this._toolbar.find(".btn-group.right[data-tb-order]")
                .toArray()
                .sort((a,b)=> parseInt(a.getAttribute("data-tb-order")) - parseInt(b.getAttribute("data-tb-order")));

            if(sortedElements.length>0){
                jQuery(sortedElements[0]).addClass("ml-auto");
            }
        }

        public removeChild(child: JQuery): void {
            child.remove();
        }

        public getElement(): JQuery {
            return this._toolbar;
        }
    }
} 
