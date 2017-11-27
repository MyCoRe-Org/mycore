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

/// <reference path="../../../Utils.ts" />
/// <reference path="ButtonController.ts" />
/// <reference path="../events/DropdownButtonPressedEvent.ts" />
/// <reference path="../model/ToolbarDropdownButton.ts" />
/// <reference path="../view/group/GroupView.ts" />
/// <reference path="../view/dropdown/DropdownView.ts" />
/// <reference path="../view/ToolbarViewFactory.ts" />

namespace mycore.viewer.widgets.toolbar {
    export class DropdownButtonController extends ButtonController {

        constructor(_groupMap:MyCoReMap<string, GroupView>, private _dropdownButtonViewMap:MyCoReMap<string, DropdownView>, private __mobile = false) {
            super(_groupMap, _dropdownButtonViewMap, __mobile);
        }

        public childAdded(parent:any, component:any):void {
            super.childAdded(parent, component);
            var button = <ToolbarDropdownButton>component;
            var componentId = component.getProperty("id").value;
            var childrenProperty = button.getProperty("children");

            childrenProperty.addObserver(this);

            var dropdownView = this._dropdownButtonViewMap.get(componentId);
            dropdownView.updateChilds(childrenProperty.value);

            this.updateChildEvents(button, childrenProperty.value);
        }

        public updateChildEvents(button:ToolbarDropdownButton, childs:Array<ToolbarDropdownButtonChild>):void {
            var dropdownView = this._dropdownButtonViewMap.get(button.id);
            var that = this;
            if (button.largeContent || this.__mobile) {
                dropdownView.getElement().bind("change", function (modelElement:ToolbarDropdownButton) {
                    return function (e) {
                        var jqTarget = jQuery(e.target);
                        var select = jqTarget.find(":selected");
                        if (that.__mobile) {
                            jqTarget.val([]);
                        }
                        that.eventManager.trigger(new events.DropdownButtonPressedEvent(button, select.attr("data-id")));
                    };
                }(button));
            } else {
                var childArray:Array<ToolbarDropdownButtonChild> = childs;
                for (var childIndex in childArray) {
                    var view = dropdownView.getChildElement(childArray[childIndex].id);
                    view.bind("click", function (modelElement:ToolbarDropdownButtonChild) {
                        return function () {
                            that.eventManager.trigger(new events.DropdownButtonPressedEvent(button, modelElement.id))
                        };
                    }(childArray[childIndex]));

                }
            }

        }

        public childRemoved(parent:any, component:any):void {
            super.childRemoved(parent, component);
        }


        public propertyChanged(_old:ViewerProperty<any>, _new:ViewerProperty<any>) {
            if (_new.name == "children") {
                this._dropdownButtonViewMap.get(_new.from.getProperty("id").value).updateChilds(_new.value);
                this.updateChildEvents(_new.from, _new.value);
            } else {
                super.propertyChanged(_old, _new);
            }
        }

        public createButtonView(dropdown:ToolbarDropdownButton):DropdownView {
            if (!this.__mobile && dropdown.largeContent) {
                return ToolbarViewFactoryImpl.createLargeDropdownView(dropdown.id);
            } else {
                return ToolbarViewFactoryImpl.createDropdownView(dropdown.id);
            }
        }

    }
}
