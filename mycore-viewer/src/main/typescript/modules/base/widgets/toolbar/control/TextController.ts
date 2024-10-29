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
import { TextView } from "../view/text/TextView";
import { GroupView } from "../view/group/GroupView";
import { ToolbarText } from "../model/ToolbarText";
import { ToolbarViewFactory } from "../view/ToolbarViewFactory";


export class TextController implements ContainerObserver<ToolbarGroup, ToolbarComponent>, ViewerPropertyObserver<any> {

  constructor(private _groupMap: MyCoReMap<string, GroupView>, private _textViewMap: MyCoReMap<string, TextView>, private _mobile: boolean) {
  }

  public childAdded(parent: any, component: any): void {
    const group = parent as ToolbarGroup;
    const groupView = this._groupMap.get(group.name);
    const componentId = component.getProperty("id").value;
    const text = component as ToolbarText;

    const textView = this.createTextView(componentId);

    const textProperty = text.getProperty("text");
    textProperty.addObserver(this);
    textView.updateText(textProperty.value);

    groupView.addChild(textView.getElement());

    this._textViewMap.set(componentId, textView);
  }

  public childRemoved(parent: any, component: any): void {
    const componentId = component.getProperty("id").value;
    this._textViewMap.get(componentId).getElement().remove();

    component.getProperty("text").removeObserver(this);
  }

  public propertyChanged(_old: ViewerProperty<any>, _new: ViewerProperty<any>) {
    const textId = _new.from.getProperty("id").value;
    if (_old.name == "text" && _new.name == "text") {
      this._textViewMap.get(textId).updateText(_new.value);
    }
  }

  public createTextView(id: string): TextView {
    return ((window as any).ToolbarViewFactoryImpl as ToolbarViewFactory).createTextView(id);
  }

}

