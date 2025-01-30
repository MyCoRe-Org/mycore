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

import { ChapterTreeView } from "./ChapterTreeView";
import { ChapterTreeInputHandler } from "./ChapterTreeInputHandler";
import { MyCoReMap } from "../../Utils";

export class MobileChapterTreeView implements ChapterTreeView {

  constructor(private _container: HTMLElement, private _inputHandler: ChapterTreeInputHandler, className: string = "chapterTreeDesktop") {
    this.list = document.createElement("ul");

    this.list.classList.add("mobileListview")
    this.levelMap = new MyCoReMap<string, number>();
    _container.append(this.list);
  }

  private list: HTMLElement;
  private levelMap: MyCoReMap<string, number>;
  private static LEVEL_MARGIN = 15;

  addNode(parentId: string, id: string, label: string, childLabel: string, expandable: boolean) {
    if (label === childLabel) {
      childLabel = '';
    }
    const newElement = document.createElement("li");
    const labelElement = document.createElement("a");

    labelElement.classList.add("label");
    labelElement.innerText = label;
    labelElement.setAttribute("data-id", id);
    newElement.appendChild(labelElement);

    const childlabelElement = document.createElement("a");

    childlabelElement.innerText = childLabel;
    childlabelElement.classList.add("childLabel");
    newElement.append(childlabelElement);

    newElement.setAttribute("data-id", id);

    let level = 0;
    if (parentId != null && this.levelMap.has(parentId)) {
      level = this.levelMap.get(parentId) + 1;
    }
    this.levelMap.set(id, level);

    labelElement.style.paddingLeft = (15*level) + "px";

    this.list.append(newElement);
    this._inputHandler.registerNode(newElement, id);
  }

  setOpened(id: string, opened: boolean) {
    return;
  }

  setSelected(id: string, selected: boolean) {
    return;
  }

  jumpTo(id: string) {
    return;
  }

}


