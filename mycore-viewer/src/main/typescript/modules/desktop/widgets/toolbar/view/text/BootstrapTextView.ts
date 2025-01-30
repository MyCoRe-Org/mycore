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


import { TextView } from "../../../../../base/widgets/toolbar/view/text/TextView";

export class BootstrapTextView implements TextView {

  constructor(private _id: string) {
    this.element = document.createElement("p");
    this.element.classList.add("navbar-text");
  }

  private element: HTMLElement;

  updateText(text: string): void {
    this.element.innerText = text;
  }

  getElement(): HTMLElement {
    return this.element;
  }
}

