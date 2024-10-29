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

import {ChapterTreeView} from "./ChapterTreeView";
import {ChapterTreeInputHandler} from "./ChapterTreeInputHandler";
import {MyCoReMap} from "../../Utils";

export class MobileChapterTreeView implements ChapterTreeView {

    constructor(private _container: JQuery, private _inputHandler: ChapterTreeInputHandler, className: string = "chapterTreeDesktop") {
        this.list = jQuery("<ul></ul>");

        this.list.addClass("mobileListview")
        this.levelMap = new MyCoReMap<string, number>();
        this.list.appendTo(_container);
    }

    private list: JQuery;
    private levelMap: MyCoReMap<string, number>;
    private static LEVEL_MARGIN = 15;

    addNode(parentId: string, id: string, label: string, childLabel: string, expandable: boolean) {
        if (label === childLabel) {
            childLabel = '';
        }
        const newElement = jQuery("<li></li>");
        const labelElement = jQuery("<a></a>");

        labelElement.addClass("label");
        labelElement.text(label);
        labelElement.attr("data-id", id);
        labelElement.appendTo(newElement);

        const childlabelElement = jQuery("<a></a>");

        childlabelElement.text(childLabel);
        childlabelElement.addClass("childLabel");
        childlabelElement.appendTo(newElement);

        newElement.attr("data-id", id);

        let level = 0;
        if (parentId != null && this.levelMap.has(parentId)) {
            level = this.levelMap.get(parentId) + 1;
        }
        this.levelMap.set(id, level);

        labelElement.css({"padding-left": (15 * level) + "px"})

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


