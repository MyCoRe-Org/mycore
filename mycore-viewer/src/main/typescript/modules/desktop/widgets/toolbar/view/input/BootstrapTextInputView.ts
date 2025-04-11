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

import { TextInputView } from "../../../../../base/widgets/toolbar/view/input/TextInputView";


export class BootstrapTextInputView implements TextInputView {

  constructor(private _id: string) {
    this.element = document.createElement("form");
    this.element.classList.add("navbar-form");
    this.element.style.display = "inline-block";

    this.childText = document.createElement("input");
    this.childText.classList.add("form-control");
    this.childText.setAttribute("type", "text");

    this.element.append(this.childText);

    this.childText.addEventListener('keydown', (e) => {
      if (e.keyCode == 13) {
        e.preventDefault();
      }
    });

    this.childText.addEventListener('keyup', (e) => {
      console.log(e);
      if (e.keyCode) {
        if (e.keyCode == 27) { // Unfocus when pressing escape
          this.childText.value = "";
          this.childText.blur()
        }
      }

      if (this.onChange != null) {
        this.onChange();
      }
    });
  }

  private element: HTMLElement;
  private childText: HTMLInputElement;

  public onChange: () => void = null;

  updateValue(value: string): void {
    this.childText.value = value;
  }

  getValue(): string {
    return this.childText.value;
  }

  getElement(): HTMLElement {
    return this.element;
  }

  updatePlaceholder(placeHolder: string): void {
    this.childText.setAttribute("placeholder", placeHolder);
  }
}

