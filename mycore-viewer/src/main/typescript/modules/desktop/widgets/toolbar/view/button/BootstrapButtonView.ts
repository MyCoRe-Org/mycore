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


import { ButtonView } from "../../../../../base/widgets/toolbar/view/button/ButtonView";

export class BootstrapButtonView implements ButtonView {

  constructor(id: string) {
    this._buttonElement = document.createElement("button");
    this._buttonElement.setAttribute("data-id", id);
    this._buttonElement.classList.add("btn", "btn-secondary", "navbar-btn");

    this._buttonLabel = document.createElement("span");
    this._buttonElement.append(this._buttonLabel);

    this._icon = document.createElement("i");
    this._attached = false;
    this._lastIconClass = "";
    this._lastButtonClass = null;
  }

  public _buttonElement: HTMLElement;
  private _icon: HTMLElement;
  private _attached: boolean;
  private _buttonLabel: HTMLElement;
  private _lastIconClass: string;
  private _lastButtonClass: string;

  public updateButtonLabel(label: string): void {
    this._buttonLabel.innerText = label;
    if (label.length > 0) {
      this._icon.classList.add("textpresent");
    } else {
      this._icon.classList.remove("textpresent");
    }
  }

  public updateButtonTooltip(tooltip: string): void {
    this._buttonElement.setAttribute("title", tooltip);
  }

  public updateButtonIcon(icon: string): void {
    if (!this._attached && icon != null) {
      this._buttonElement.prepend(this._icon);
      this._attached = true;
    } else if (this._attached && icon == null) {
      this._icon.remove();
      this._attached = false;
      return;
    }

    this._icon.classList.remove(`fa-${this._lastIconClass}`);
    if(this._lastIconClass) {
      this._icon.classList.remove(this._lastIconClass);
    }
    this._icon.classList.remove(`icon-${this._lastIconClass}`);

    this._icon.classList.add('fas');
    this._icon.classList.add(`fa-${icon}`);

    this._lastIconClass = icon;
  }

  updateButtonClass(buttonClass: string): void {
    if (this._lastButtonClass != null) {
      this._buttonElement.classList.remove("btn-" + this._lastButtonClass);
    }

    this._buttonElement.classList.add("btn-" + buttonClass);
    this._lastButtonClass = buttonClass;
  }

  updateButtonActive(active: boolean): void {
    const isActive = this._buttonElement.classList.contains("active");

    if (active && !isActive) {
      this._buttonElement.classList.add("active");
    }

    if (!active && isActive) {
      this._buttonElement.classList.remove("active");
    }
  }

  updateButtonDisabled(disabled: boolean): void {
    const isDisabled = this._buttonElement.getAttribute("disabled") == "disabled";

    if (disabled && !isDisabled) {
      this._buttonElement.setAttribute("disabled", "disabled");
    }

    if (!disabled && isDisabled) {
      this._buttonElement.removeAttribute("disabled");
    }
  }


  public getElement(): HTMLElement[] {
    return [this._buttonElement];
  }
}


