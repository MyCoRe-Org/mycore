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

import { IviewChapterTree } from "./IviewChapterTree";

export interface ChapterTreeInputHandler {
  // called from ctrl
  register(ctrl: IviewChapterTree): void;

  /**
  * Called if a chapter was inserted in the tree. 
  * Use this to register events. e.g. onclick.
  * 
  * node is the label of the node.
  * id is the id of the node.
  */
  registerNode(node: HTMLElement, id: string): void;

  /**
   * Called if a chapter was inserted and the chapter can hold children.
   * Use this to register events. e.g. onclick.
   * 
   * expander is the "arrow" to expand the tree.
   * id is the id of the node with has the expander.
   */
  registerExpander(expander: HTMLElement, id: string): void;
}

export class DefaultChapterTreeInputHandler implements ChapterTreeInputHandler {

  constructor() {
  }



  private _ctrl: IviewChapterTree;

  register(ctrl: IviewChapterTree): void {
    this._ctrl = ctrl;
  }

  registerNode(node: HTMLElement, id: string): void {
    node.addEventListener("click", () => {
      const newSelectedChapter = this._ctrl.getChapterById(id);
        this._ctrl.setChapterSelected(newSelectedChapter);
    });
  }

  registerExpander(expander: HTMLElement, id: string): void {
    expander.addEventListener("click", () => {
        const chapterToChange = this._ctrl.getChapterById(id);
        this._ctrl.setChapterExpanded(chapterToChange, !this._ctrl.getChapterExpanded(chapterToChange));
    });
  }
}


