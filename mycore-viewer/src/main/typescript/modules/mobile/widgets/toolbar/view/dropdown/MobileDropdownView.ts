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

    export class MobileDropdownView implements DropdownView {

        constructor(private _id:string) {
            this._buttonElement = jQuery("<div></div>");
            this._buttonElement.addClass("dropdown");

            this._buttonElementInner = jQuery("<span></span>");
            this._buttonElementInner.addClass("fas fa-bars");
            this._buttonElementInner.appendTo(this._buttonElement);

            this._dropdown = jQuery("<select></select>");
            this._dropdown.appendTo(this._buttonElement);

            var defaultChild = jQuery("<option selected disabled hidden value=''></option>");
            defaultChild.css({"display": "none"});
            defaultChild.appendTo(this._dropdown);

            /*
             this._buttonElement.addClass("select-choice-min")
             this._buttonElement.attr("data-mini", "true");
             this._buttonElement.attr("data-iconpos","left");
             this._buttonElement.attr("data-icon", "th-list");     */
            //this._buttonElement.selectmenu();
            this._childMap = new MyCoReMap<string, JQuery>();
        }

        private _buttonElement:JQuery;
        private _buttonElementInner:JQuery;
        private _dropdown:JQuery;

        public updateButtonLabel(label:string):void {
        }

        public updateButtonTooltip(tooltip:string):void {
        }

        public updateButtonIcon(icon:string):void {
        }

        public updateButtonClass(buttonClass:string):void {
        }

        public updateButtonActive(active:boolean):void {
        }

        public updateButtonDisabled(disabled:boolean):void {
        }

        private _childMap:MyCoReMap<string, JQuery>;

        public updateChilds(childs:Array<{id:string;label:string
        }>):void {
            this._childMap.forEach(function (key, val) {
                val.remove();
            });
            this._childMap.clear();

            for (var childIndex in childs) {
                var current:{id:string;label:string
                } = childs[childIndex];
                var newChild = jQuery("<option value='" + current.id + "' data-id=\"" + current.id + "\">" + current.label + "</option>");
                this._childMap.set(current.id, newChild);
                newChild.appendTo(this._dropdown);
            }
        }

        public getChildElement(id:string):JQuery {
            return this._childMap.get(id) || null;
        }

        public getElement():JQuery {
            return jQuery().add(this._buttonElement);
        }

    }
}
