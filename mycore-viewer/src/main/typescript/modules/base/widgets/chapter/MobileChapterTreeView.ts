/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="../../Utils.ts" />
/// <reference path="ChapterTreeView.ts" />
/// <reference path="ChapterTreeInputHandler.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export class MobileChapterTreeView implements ChapterTreeView {

        constructor(private _container:JQuery, private _inputHandler:ChapterTreeInputHandler, className:string = "chapterTreeDesktop") {
            this.list = jQuery("<ul></ul>");

            this.list.addClass("mobileListview")
            this.levelMap = new MyCoReMap<string, number>();
            this.list.appendTo(_container);
        }

        private list:JQuery;
        private levelMap:MyCoReMap<string, number>;
        private static LEVEL_MARGIN = 15;

        addNode(parentId:string, id:string, label:string, childLabel: string, expandable:boolean) {
            var newElement = jQuery("<li></li>");
            var labelElement = jQuery("<a></a>");

            labelElement.addClass("label");
            labelElement.text(label);
            labelElement.attr("data-id", id);
            labelElement.appendTo(newElement);
            
            var childlabelElement = jQuery("<a></a>");

            childlabelElement.text(childLabel);
            childlabelElement.addClass("childLabel");
            childlabelElement.appendTo(newElement);
            
            newElement.attr("data-id", id);

            var level = 0;
            if (parentId != null && this.levelMap.has(parentId)) {
                level = this.levelMap.get(parentId) + 1;
            }
            this.levelMap.set(id, level);

            labelElement.css({"padding-left": (15 * level) + "px"})

            this.list.append(newElement);
            this._inputHandler.registerNode(newElement, id);
        }

        setOpened(id:string, opened:boolean) {
            return;
        }

        setSelected(id:string, selected:boolean) {
            return;
        }

        jumpTo(id:string) {
            return;
        }

    }

}
