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
import {getElementHeight} from "../../Utils";

export class DesktopChapterTreeView implements ChapterTreeView {

  constructor(private _container: HTMLElement, private _inputHandler: ChapterTreeInputHandler, className: string = "chapterTreeDesktop") {
    this.list = document.createElement("ol");
    this.list.classList.add(className);
    this.list.classList.add("list-group");
    this._container.append(this.list);
  }

  private static CLOSE_ICON_CLASS: string = "fa-chevron-right";
  private static OPEN_ICON_CLASS: string = "fa-chevron-down";
  public list: HTMLElement;

  public addNode(parentId: string, id: string, label: string, childLabel: string, expandable: boolean) {
    // Resolves the Parent
    const parentElement = this.getParent(parentId);

    // Creates the Node
    if (label === childLabel) {
      childLabel = '';
    }
    const node = this.createNode(id, label, childLabel, expandable);
    parentElement.append(node);
  }

  /**
   * Resolves the List of a Parent or creates it.
   */
  public getParent(parentId: string): HTMLElement {
    if (!parentId) {
      return this.list;
    }
    const escapedId = CSS.escape(parentId);
    let parentElement = document.querySelector<HTMLElement>(`ol[data-id='${escapedId}']`);
    if (parentElement) {
      return parentElement;
    }
    const liElement = document.querySelector<HTMLElement>(`li[data-id='${escapedId}']`);
    if (!liElement || liElement.parentElement) {
      return this.list;
    }
    const childrenList = document.createElement("ol");
    childrenList.dataset.id = parentId;
    childrenList.dataset.opened = "true";
    liElement.parentElement.insertBefore(childrenList, liElement.nextSibling);
    return childrenList;
  }

  public createNode(id: string, label: string, childLabel: string, expandable: boolean): HTMLElement {
    const insertedNode = document.createElement("li");
    const labelElement = document.createElement("a");
    labelElement.setAttribute("title", label);
    labelElement.innerText = label;
    const childLabelElement = document.createElement("span");
    childLabelElement.innerText = childLabel;
    childLabelElement.classList.add("childLabel");
    insertedNode.append(labelElement);
    insertedNode.append(childLabelElement);
    insertedNode.classList.add("list-group-item");
    insertedNode.setAttribute("data-id", id);
    insertedNode.setAttribute("data-opened", "true");
    this._inputHandler.registerNode(labelElement, id);

    if (expandable) {
      //const expander = jQuery("<span class=\"expander fas " + DesktopChapterTreeView.OPEN_ICON_CLASS + "\"></span>");
      const expander = document.createElement("span");
      expander.classList.add("expander", "fas", DesktopChapterTreeView.OPEN_ICON_CLASS);

      this._inputHandler.registerExpander(expander, id);
      insertedNode.prepend(expander);
    }
    return  insertedNode;
  }


  public setOpened(id: string, opened: boolean) {
    const escapedId = CSS.escape(id);
    const liElem = this.list.querySelector("li[data-id='" + escapedId + "']");
    const olElem = this.list.querySelector("ol[data-id='" + escapedId + "']");

    if(liElem) {
      liElem.setAttribute("data-opened", opened.toString());
    }

    if(olElem) {
      olElem.setAttribute("data-opened", opened.toString());
    }

    const span = this.list.querySelector("li[data-id='" + escapedId + "'] span.expander");

    if (opened) {
      span?.classList.add(DesktopChapterTreeView.OPEN_ICON_CLASS);
      span?.classList.remove(DesktopChapterTreeView.CLOSE_ICON_CLASS);
    } else {
      span?.classList.add(DesktopChapterTreeView.CLOSE_ICON_CLASS);
      span?.classList.remove(DesktopChapterTreeView.OPEN_ICON_CLASS);
    }
  }

  public setSelected(id: string, selected: boolean) {
    const elem = this.list.querySelector("li[data-id='" + CSS.escape(id) + "']");
    if(elem) {
      elem.setAttribute("data-selected", selected.toString());
    }
  }

  public jumpTo(id: string) {
    const elem = this.list.querySelector("li[data-id='" + CSS.escape(id) + "']") as HTMLElement;

    elem.classList.add("blink");
    setTimeout(() => {
          elem.classList.remove("blink");
        }, 500
    );

    const realElementPosition = elem.offsetTop - this._container.offsetTop;
    let move = 0;
    if (realElementPosition < 0) {
      move = realElementPosition - 10;
    } else {
      const containerHeight = getElementHeight(this._container);
      const elementHeight = getElementHeight(elem);
      if ((realElementPosition + elementHeight + 10) > containerHeight) {
        move = (realElementPosition - containerHeight) + elementHeight + 10;
      } else {
        return;
      }
    }

    this._container.scrollBy(0, move);
  }
}


