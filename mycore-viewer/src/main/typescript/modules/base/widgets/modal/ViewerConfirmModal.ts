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

///<reference path="ViewerModalWindow.ts"/>
namespace mycore.viewer.widgets.modal {

    export class ViewerConfirmModal extends IviewModalWindow {

        constructor(_mobile:boolean, confirmTitle:string, confirmText:string, callback:Function, parent:HTMLElement=document.body) {
            super(_mobile, confirmTitle, parent);
            this.modalHeader.children("h4").addClass("text-info");
            this.modalBody.append("<p><span data-i18n='" + confirmText +"'>" + confirmText + "</span></p>");

            this.modalFooter.empty();

            this.createButton(true, callback);
            this.createButton(false, callback);
        }

        private createButton(confirm:boolean, callback:Function):void {
            let key = confirm ? "yes" : "no";
            let button = jQuery("<a data-i18n='modal." + key + "'></a>");
            button.attr("type", "button");
            button.addClass("btn btn-default");
            button.appendTo(this.modalFooter);

            var that = this;
            button.click(()=> {
                if(callback) {
                    callback(confirm);
                }
                that.hide();
            });
        }
    }

}
