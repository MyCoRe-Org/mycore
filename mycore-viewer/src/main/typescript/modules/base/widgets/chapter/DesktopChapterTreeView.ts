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
/// <reference path="ChapterTreeView.ts" />
/// <reference path="ChapterTreeInputHandler.ts" />

namespace mycore.viewer.widgets.chaptertree {

    export class DesktopChapterTreeView implements ChapterTreeView {

        constructor(private _container:JQuery, private _inputHandler:ChapterTreeInputHandler, className:string = "chapterTreeDesktop") {
            this.list = jQuery("<ol><ol>");
            this.list.addClass(className);
            this.list.addClass("list-group");
            this._container.append(this.list);
        }

        private static CLOSE_ICON_CLASS:string = "glyphicon-chevron-right";
        private static OPEN_ICON_CLASS:string = "glyphicon-chevron-down";
        public list:JQuery;

        public addNode(parentId:string, id:string, label:string, childLabel:string, expandable:boolean) {
            // Resolves the Parent
            var parentElement = this.getParent(parentId);

            // Creates the Node
            var nodeToAdd = this.createNode(id, label, childLabel, expandable);

            parentElement.append(nodeToAdd);
        }

        /**
         * Resolves the List of a Parent or creates it.
         */
        public getParent(parentId:string):JQuery {
            var parentElement:JQuery;
            if (parentId != null) {
                parentElement = this.list.find("ol[data-id='" + parentId + "']");
                // Creates ol for children if not exist
                if (parentElement.length == 0) {
                    parentElement = this.list.find("li[data-id='" + parentId + "']");
                    var childrenList = jQuery("<ol></ol>");
                    childrenList.attr("data-id", parentId);
                    childrenList.attr("data-opened", true);
                    childrenList.insertAfter(parentElement);
                    parentElement = childrenList;
                }
            } else {
                parentElement = this.list;
            }
            return parentElement;
        }


        public createNode(id:string, label:string, childLabel:string, expandable:boolean):JQuery {
            var insertedNode = jQuery("<li></li>");
            var labelElement = jQuery("<a title='" + label + "'></a>").text(label);
            var childLabelElement = jQuery("<span>" + childLabel + "</span>");
            childLabelElement.addClass("childLabel");
            insertedNode.append(labelElement);
            insertedNode.append(childLabelElement);
            insertedNode.addClass("list-group-item");
            insertedNode.attr("data-id", id);
            insertedNode.attr("data-opened", true);
            this._inputHandler.registerNode(labelElement, id);

            if (expandable) {
                var expander = jQuery("<span class=\"expander glyphicon " + DesktopChapterTreeView.OPEN_ICON_CLASS + "\"></span>");
                insertedNode.prepend(expander);
                this._inputHandler.registerExpander(expander, id);
            }
            return insertedNode;
        }


        public setOpened(id:string, opened:boolean) {
            var liElem = this.list.find("li[data-id='" + id + "']").attr("data-opened", opened.toString());
            var olElem = this.list.find("ol[data-id='" + id + "']").attr("data-opened", opened.toString());

            var span = this.list.find("li[data-id='" + id + "'] span.expander");

            if (opened) {
                span.removeClass(DesktopChapterTreeView.CLOSE_ICON_CLASS);
                span.addClass(DesktopChapterTreeView.OPEN_ICON_CLASS);
            } else {
                span.removeClass(DesktopChapterTreeView.OPEN_ICON_CLASS);
                span.addClass(DesktopChapterTreeView.CLOSE_ICON_CLASS);
            }
        }

        public setSelected(id:string, selected:boolean) {
            var elem = this.list.find("li[data-id='" + id + "']").attr("data-selected", selected.toString());
        }

        public jumpTo(id:string) {
            var elem = this.list.find("li[data-id='" + id + "']");

            elem.addClass("blink");
            setTimeout(()=> {
                    elem.removeClass("blink");
                }, 500
            );

            var realElementPosition = elem.position().top - this._container.position().top;
            var move = 0;
            if (realElementPosition < 0) {
                move = realElementPosition - 10;
            } else {
                var containerHeight = this._container.height();
                var elementHeight = elem.height();
                if ((realElementPosition + elementHeight+10) > containerHeight) {
                    move = (realElementPosition - containerHeight) + elementHeight + 10;
                } else {
                    return;
                }
            }


            this._container.scrollTop(this._container.scrollTop() + move);
        }
    }

}
