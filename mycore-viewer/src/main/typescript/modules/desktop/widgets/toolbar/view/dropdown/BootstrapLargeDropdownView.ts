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


import { DropdownView } from "../../../../../base/widgets/toolbar/view/dropdown/DropdownView";
import { MyCoReMap } from "../../../../../base/Utils";

export class BootstrapLargeDropdownView implements DropdownView {

  constructor(private _id: string) {
    this._buttonElement = document.createElement("select");
    this._buttonElement.classList.add("btn", "btn-secondary", "navbar-btn", "dropdown");
    this._childMap = new MyCoReMap<string, HTMLElement>();
  }

  private _buttonElement: HTMLElement;

  public updateButtonLabel(label: string): void {
  }

  public updateButtonTooltip(tooltip: string): void {
  }

  public updateButtonIcon(icon: string): void {
  }

  public updateButtonClass(buttonClass: string): void {
  }

  public updateButtonActive(active: boolean): void {
  }

  public updateButtonDisabled(disabled: boolean): void {
  }

  private _childMap: MyCoReMap<string, HTMLElement>;

  public updateChilds(childs: Array<{
    id: string; label: string
  }>): void {
    this._childMap.forEach(function(key, val) {
      val.remove();
    });
    this._childMap.clear();

    for (let childIndex in childs) {
      const current: {
        id: string; label: string
      } = childs[childIndex];
      const newChild = document.createElement("option");
        newChild.value = current.id;
        newChild.setAttribute("data-id", current.id);
        newChild.innerText = current.label;

      this._childMap.set(current.id, newChild);
      this._buttonElement.append(newChild);
    }
  }

  public getChildElement(id: string): HTMLElement[] {
    const child = this._childMap.get(id);
    return child == null ? [] : [child];
  }

  public getElement(): HTMLElement[] {
    return [this._buttonElement];
  }

}

