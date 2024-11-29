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


import { ButtonController } from "./ButtonController";
import { MyCoReMap, ViewerProperty } from "../../../Utils";
import { GroupView } from "../view/group/GroupView";
import { DropdownView } from "../view/dropdown/DropdownView";
import { ToolbarDropdownButton, ToolbarDropdownButtonChild } from "../model/ToolbarDropdownButton";
import { DropdownButtonPressedEvent } from "../events/DropdownButtonPressedEvent";
import { ToolbarViewFactory } from "../view/ToolbarViewFactory";


export class DropdownButtonController extends ButtonController {

  constructor(_groupMap: MyCoReMap<string, GroupView>, private _dropdownButtonViewMap: MyCoReMap<string, DropdownView>, private __mobile = false) {
    super(_groupMap, _dropdownButtonViewMap, __mobile);
  }

  public childAdded(parent: any, component: any): void {
    super.childAdded(parent, component);
    const button = component as ToolbarDropdownButton;
    const componentId = component.getProperty("id").value;
    const childrenProperty = button.getProperty("children");

    childrenProperty.addObserver(this);

    const dropdownView = this._dropdownButtonViewMap.get(componentId);
    dropdownView.updateChilds(childrenProperty.value);

    this.updateChildEvents(button, childrenProperty.value);
  }

  public updateChildEvents(button: ToolbarDropdownButton, childs: Array<ToolbarDropdownButtonChild>): void {
    const dropdownView = this._dropdownButtonViewMap.get(button.id);
    const that = this;
    if (button.largeContent || this.__mobile) {
      dropdownView.getElement().bind("change", function(modelElement: ToolbarDropdownButton) {
        return function(e) {
          const jqTarget = jQuery(e.target);
          const select = jqTarget.find(":selected");
          if (that.__mobile) {
            jqTarget.val([]);
          }
          that.eventManager.trigger(new DropdownButtonPressedEvent(button, select.attr("data-id")));
        };
      }(button));
    } else {
      const childArray: Array<ToolbarDropdownButtonChild> = childs;
      childArray.forEach((child) => {
        const view = dropdownView.getChildElement(child.id);
        view.bind("click", () => {
          that.eventManager.trigger(new DropdownButtonPressedEvent(button, child.id))
        });
      });
    }

  }

  public childRemoved(parent: any, component: any): void {
    super.childRemoved(parent, component);
  }


  public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
    if (_new.name == "children") {
      this._dropdownButtonViewMap.get(_new.from.getProperty("id").value).updateChilds(_new.value);
      this.updateChildEvents(_new.from, _new.value);
    } else {
      super.propertyChanged(_old, _new);
    }
  }

  public createButtonView(dropdown: ToolbarDropdownButton): DropdownView {
    if (!this.__mobile && dropdown.largeContent) {
      return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createLargeDropdownView(dropdown.id);
    } else {
      return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createDropdownView(dropdown.id);
    }
  }

}

