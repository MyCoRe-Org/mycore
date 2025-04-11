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

export class ViewerErrorModal extends ViewerModalWindow {

  constructor(_mobile: boolean, errorTitle: string, errorText: string, imageUrl?: string, parent: HTMLElement = document.body) {
    super(_mobile, errorTitle, parent);
    this.modalHeader.querySelectorAll(">h4").forEach(el => el.classList.add("text-danger"));

    const errorImageHolder = document.createElement("div");
    errorImageHolder.classList.add("error-image-holder");
    this.modalBody.append(errorImageHolder);

    if(imageUrl != null) {
      const img = document.createElement("img")
      img.classList.add("thumbnail", "error-image");
      img.src = imageUrl;
        errorImageHolder.append(img);
    }

    const span = document.createElement("span");
    span.innerText = errorText;
    errorImageHolder.append(span);
  }
}


