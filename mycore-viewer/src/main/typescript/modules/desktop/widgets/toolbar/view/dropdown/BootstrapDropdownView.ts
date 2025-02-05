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
import { BootstrapButtonView } from "../button/BootstrapButtonView";
import { MyCoReMap } from "../../../../../base/Utils";

export class BootstrapDropdownView extends BootstrapButtonView implements DropdownView {

  constructor(_id: string) {
    super(_id);
    this._buttonElement.setAttribute("data-bs-toggle", "dropdown");
    this._buttonElement.classList.add("dropdown-toggle");
    this._caret = document.createElement("span");
    this._caret.classList.add("caret");
    this._buttonElement.append(this._caret);

    this._dropdownMenu = document.createElement("ul");
    this._dropdownMenu.classList.add("dropdown-menu");
    this._dropdownMenu.setAttribute("role", "menu");
    this._childMap = new MyCoReMap<string, HTMLElement[]>();
  }

  private _caret: HTMLElement;
  private _dropdownMenu: HTMLElement;
  private _childMap: MyCoReMap<string, HTMLElement[]>;

  public updateChilds(childs: Array<{
    id: string; label: string; isHeader?: boolean; icon?: string
  }>): void {
    this._childMap.forEach(function(key, val) {
      val.forEach(el => el.remove());
    });
    this._childMap.clear();

    let first = true;
    for (let childIndex in childs) {
      const current: { id: string; label: string; isHeader?: boolean; icon?: string } = childs[childIndex];

      let newChildren: HTMLElement[] = [];
      if ("isHeader" in current && current.isHeader) {
        if (!first) {
          const dividerElement = document.createElement("li");
            dividerElement.classList.add("divider");
            dividerElement.setAttribute("value", "divider-" + current.id);
            dividerElement.setAttribute("data-id", "divider-" + current.id);
            newChildren.push(dividerElement);
        }
        const headerElement = document.createElement("li");
        headerElement.classList.add("disabled");
        headerElement.setAttribute("value", "divider-" + current.id);
        headerElement.setAttribute("data-id", "divider-" + current.id);
        const anchor = document.createElement("a");
        anchor.classList.add("dropdown-item");
        anchor.textContent = current.label;
        headerElement.append(anchor);
      } else {
        const anchor = document.createElement("a");
        anchor.classList.add("dropdown-item");
        anchor.textContent = current.label;
        const li = document.createElement("li");
        li.setAttribute("value", current.id);
        li.setAttribute("data-id", current.id);
        if ("icon" in current) {
          const icon = document.createElement("i");
          icon.classList.add("fas");
          icon.classList.add("fa-" + current.icon);
          icon.classList.add("dropdown-icon");
          anchor.prepend(icon);
        }
        li.append(anchor);
        newChildren.push(li);
      }
      this._childMap.set(current.id, newChildren);
      newChildren.forEach(el => this._dropdownMenu.append(el));

      if (first) {
        first = false;
      }
    }
  }

  public getChildElement(id: string): HTMLElement[] {
    return this._childMap.get(id) || null;
  }

  public getElement(): HTMLElement[] {
    return [this._buttonElement, this._dropdownMenu];
  }

}

