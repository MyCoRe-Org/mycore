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
    this._buttonElement.attr("data-toggle", "dropdown");
    this._buttonElement.addClass("dropdown-toggle");
    this._caret = jQuery("<span></span>");
    this._caret.addClass("caret");
    this._caret.appendTo(this._buttonElement);

    this._dropdownMenu = jQuery("<ul></ul>");
    this._dropdownMenu.addClass("dropdown-menu");
    this._dropdownMenu.attr("role", "menu");
    this._childMap = new MyCoReMap<string, JQuery>();
  }

  private _caret: JQuery;
  private _dropdownMenu: JQuery;
  private _childMap: MyCoReMap<string, JQuery>;

  public updateChilds(childs: Array<{
    id: string; label: string; isHeader?: boolean; icon?: string
  }>): void {
    this._childMap.forEach(function(key, val) {
      val.remove();
    });
    this._childMap.clear();

    let first = true;
    for (let childIndex in childs) {
      const current: { id: string; label: string; isHeader?: boolean; icon?: string } = childs[childIndex];

      let newChild: JQuery = jQuery("");
      if ("isHeader" in current && current.isHeader) {
        if (!first) {
          newChild = newChild.add(jQuery("<li class='divider' value='divider-" + current.id + "' data-id=\"divider-" + current.id + "\"></li>"));
        }
        newChild = newChild.add("<li class='disabled' value='divider-" + current.id + "' data-id=\"divider-" + current.id + "\"><a class='dropdown-item'>" + current.label + "</a></li>");
      } else {
        const anchor = jQuery("<a class='dropdown-item'>" + current.label + "</a>");
        newChild = jQuery(jQuery("<li value='" + current.id + "' data-id=\"" + current.id + "\"></li>"));


        if ("icon" in current) {
          const icon = jQuery(`<i class="fas fa-${current.icon} dropdown-icon"></i>`);
          anchor.prepend(icon);
        }
        newChild.append(anchor);
      }


      this._childMap.set(current.id, newChild);
      newChild.appendTo(this._dropdownMenu);

      if (first) {
        first = false;
      }
    }
  }

  public getChildElement(id: string): JQuery {
    return this._childMap.get(id) || null;
  }

  public getElement(): JQuery {
    return jQuery().add(this._buttonElement).add(this._dropdownMenu);
  }

}

