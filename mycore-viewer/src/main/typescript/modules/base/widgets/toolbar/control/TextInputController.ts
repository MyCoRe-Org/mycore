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


import { ToolbarGroup } from "../model/ToolbarGroup";
import { TextInputView } from "../view/input/TextInputView";
import { ContainerObserver, MyCoReMap, ViewerProperty, ViewerPropertyObserver } from "../../../Utils";
import { ToolbarComponent } from "../model/ToolbarComponent";
import { GroupView } from "../view/group/GroupView";
import { ToolbarTextInput } from "../model/ToolbarTextInput";
import { ToolbarViewFactory } from "../view/ToolbarViewFactory";

export class TextInputController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

  constructor(private _groupMap: MyCoReMap<string, GroupView>, private _textInputViewMap: MyCoReMap<string, TextInputView>) {
  }

  public childAdded(parent: any, component: any): void {
    const group = parent as ToolbarGroup;
    const groupView = this._groupMap.get(group.name);
    const componentId = component.getProperty("id").value;
    const text = component as ToolbarTextInput;

    const textView = this.createTextInputView(componentId);

    const valueProperty = text.getProperty("value");
    valueProperty.addObserver(this);
    textView.updateValue(valueProperty.value);

    const placeHolderProperty = text.getProperty("placeHolder");
    placeHolderProperty.addObserver(this);
    textView.updatePlaceholder(placeHolderProperty.value);

    groupView.addChild(textView.getElement());

    textView.onChange = () => {
      if (textView.getValue() != valueProperty.value) {
        valueProperty.value = textView.getValue();
      }
    };

    this._textInputViewMap.set(componentId, textView);
  }

  public childRemoved(parent: any, component: any): void {
    const componentId = component.getProperty("id").value;
    this._textInputViewMap.get(componentId).getElement().remove();

    component.getProperty("value").removeObserver(this);
    component.getProperty("placeHolder").removeObserver(this);
  }

  public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
    const textId = _new.from.getProperty("id").value;
    const textInputView = this._textInputViewMap.get(textId);
    if (_old.name == "value" && _new.name == "value") {
      if (textInputView.getValue() != _new.value) {
        textInputView.updateValue(_new.value);
      }
    }

    if (_old.name == "placeHolder" && _new.name == "placeHolder") {
      if (textInputView.getValue() != _new.value) {
        textInputView.updatePlaceholder(_new.value);
      }
    }
  }

  public createTextInputView(id: string): TextInputView {
    return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createTextInputView(id);
  }

}

