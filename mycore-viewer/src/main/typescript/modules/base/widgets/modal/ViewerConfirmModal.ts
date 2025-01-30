/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import { ViewerModalWindow } from "./ViewerModalWindow";

export class ViewerConfirmModal extends ViewerModalWindow {

  constructor(_mobile: boolean, confirmTitle: string, confirmText: string, callback: Function, parent: HTMLElement = document.body) {
    super(_mobile, confirmTitle, parent);
    this.modalHeader.querySelectorAll(">h4").forEach(el => {
        el.classList.add("text-info");
    });
    /*this.modalBody.append("<p><span data-i18n='" + confirmText + "'>" + confirmText + "</span></p>");*/
    const p = document.createElement("p");
    p.innerText = confirmText;
    p.setAttribute("data-i18n", confirmText);
    this.modalBody.appendChild(p);


    for (let i = 0; i < this.modalFooter.children.length; i++) {
        this.modalFooter.children[i].remove();
    }

    this.createButton(true, callback);
    this.createButton(false, callback);
  }

  private createButton(confirm: boolean, callback: Function): void {
    let key = confirm ? "yes" : "no";
    const button = document.createElement("a");
    button.setAttribute("type", "button");
    button.classList.add("btn", "btn-secondary");
    button.innerText = key;
    button.setAttribute("data-i18n", "modal." + key);
    this.modalFooter.appendChild(button);

    button.addEventListener("click", () => {
      if (callback) {
        callback(confirm);
      }
      this.hide();
    });
  }
}


