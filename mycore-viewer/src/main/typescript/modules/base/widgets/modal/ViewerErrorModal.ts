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

    export class ViewerErrorModal extends IviewModalWindow {

        constructor(_mobile:boolean, errorTitle:string,errorText:string, imageUrl?:string, parent:HTMLElement=document.body) {
            super(_mobile, errorTitle, parent);
            this.modalHeader.children("h4").addClass("text-danger");
            let img = imageUrl != null ? `<img class='thumbnail error-image' src='${imageUrl}' />` : "";
            this.modalBody.append(`<div class='error-image-holder'> ${img} <span data-i18n='" + text + "'>${errorText}</span></div>`)
        }
    }

}
