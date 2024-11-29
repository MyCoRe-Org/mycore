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


import { ContainerObserver, MyCoReMap, ViewerProperty, ViewerPropertyObserver } from "../../../Utils";
import { ToolbarGroup } from "../model/ToolbarGroup";
import { ToolbarComponent } from "../model/ToolbarComponent";
import { GroupView } from "../view/group/GroupView";
import { ButtonView } from "../view/button/ButtonView";
import { ToolbarButton } from "../model/ToolbarButton";
import { ButtonPressedEvent } from "../events/ButtonPressedEvent";
import { ToolbarViewFactory } from "../view/ToolbarViewFactory";
import { ViewerEventManager } from "../../events/ViewerEventManager";


export class ButtonController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

  constructor(private _groupMap: MyCoReMap<string, GroupView>, private _buttonViewMap: MyCoReMap<string, ButtonView>, private _mobile: boolean) {
    this._eventManager = new ViewerEventManager();
  }

  private _eventManager: ViewerEventManager;

  public get eventManager() {
    return this._eventManager;
  }

  public childAdded(parent: any, component: any): void {
    const group = <ToolbarGroup>parent;
    const groupView = this._groupMap.get(group.name);
    const componentId = component.getProperty("id").value;
    const button = component as ToolbarButton;

    const labelProperty = button.getProperty("label");
    const iconProperty = button.getProperty("icon");
    const tooltipProperty = component.getProperty("tooltip");
    const buttonClassProperty = button.getProperty("buttonClass");
    const activeProperty = button.getProperty("active");
    const disabledProperty = button.getProperty("disabled");

    labelProperty.addObserver(this);
    iconProperty.addObserver(this);
    tooltipProperty.addObserver(this);
    buttonClassProperty.addObserver(this);
    activeProperty.addObserver(this);
    disabledProperty.addObserver(this);

    const buttonView = this.createButtonView(button);

    buttonView.updateButtonTooltip(tooltipProperty.value);
    buttonView.updateButtonLabel(labelProperty.value);
    buttonView.updateButtonClass(buttonClassProperty.value);
    buttonView.updateButtonActive(activeProperty.value);
    buttonView.updateButtonDisabled(disabledProperty.value);

    buttonView.getElement().bind("click", (e) => {
      this._eventManager.trigger(new ButtonPressedEvent(button));
    });

    const iconValue = iconProperty.value;
    if (iconValue != null) {
      buttonView.updateButtonIcon(iconValue);
    }


    groupView.addChild(buttonView.getElement());
    this._buttonViewMap.set(componentId, buttonView);

  }

  public childRemoved(parent: any, component: any): void {
    const componentId = component.getProperty("id").value;
    this._buttonViewMap.get(componentId).getElement().remove();

    component.getProperty("label").removeObserver(this);
    component.getProperty("icon").removeObserver(this);
    component.getProperty("tooltip").removeObserver(this);
    component.getProperty("buttonClass").removeObserver(this);
    component.getProperty("active").removeObserver(this);
    component.getProperty("disabled").removeObserver(this);
  }

  public createButtonView(button: ToolbarButton): ButtonView {
    return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createButtonView(button.id);
  }

  public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
    // Change button
    const buttonId = _new.from.getProperty("id").value;
    if (_old.name == "label" && _new.name == "label") {
      this._buttonViewMap.get(buttonId).updateButtonLabel(_new.value);
      return;
    }

    if (_old.name == "icon" && _new.name == "icon") {
      this._buttonViewMap.get(buttonId).updateButtonIcon(_new.value);
      return;
    }

    if (_old.name == "tooltip" && _new.name == "tooltip") {
      this._buttonViewMap.get(buttonId).updateButtonTooltip(_new.value);
      return;
    }

    if (_old.name == "buttonClass" && _new.name == "buttonClass") {
      this._buttonViewMap.get(buttonId).updateButtonClass(_new.value);
      return;
    }

    if (_old.name == "active" && _new.name == "active") {
      this._buttonViewMap.get(buttonId).updateButtonActive(_new.value);
      return;
    }

    if (_old.name == "disabled" && _new.name == "disabled") {
      this._buttonViewMap.get(buttonId).updateButtonDisabled(_new.value);
      return;
    }
  }
}


