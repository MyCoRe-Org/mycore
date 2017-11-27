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

    export class MobileButtonView implements ButtonView {

        constructor(id:string) {
            this._buttonElement = jQuery("<a></a>");
            this._buttonElement.addClass("button");
            this._buttonElement.attr("data-id", id);


            this._buttonLabel = jQuery("<span></span>");
            this._buttonLabel.addClass("buttonLabel");
            this._buttonElement.append(this._buttonLabel);

            this._buttonIcon = jQuery("<span></span>");
            this._buttonElement.append(this._buttonIcon);
        }

        public _buttonElement:JQuery;
        private _buttonLabel:JQuery;
        private _buttonIcon:JQuery;
        private _lastIcon = "";

        public updateButtonLabel(label:string):void {
           this._buttonLabel.text(label);
        }

        public updateButtonTooltip(tooltip:string):void {
        }

        public updateButtonIcon(icon:string):void {
            this._buttonIcon.removeClass("fa");
            this._buttonIcon.removeClass("fa-" + this._lastIcon);
            this._buttonIcon.removeClass("icon-" + this._lastIcon);

            this._lastIcon = icon;
            this._buttonIcon.addClass("fa");
            this._buttonIcon.addClass("fa-" + icon);
            this._buttonIcon.addClass("icon-" + icon);

        }

        public updateButtonClass(buttonClass:string):void {
        }

        public updateButtonActive(active:boolean):void {

        }

        public updateButtonDisabled(disabled:boolean):void {

        }


        public getElement():JQuery {
            return this._buttonElement;
        }
    }

}
