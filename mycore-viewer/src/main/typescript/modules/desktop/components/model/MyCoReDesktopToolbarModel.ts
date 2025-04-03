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


import { MyCoReBasicToolbarModel } from "../../../base/components/model/MyCoReBasicToolbarModel";
import { ToolbarGroup } from "../../../base/widgets/toolbar/model/ToolbarGroup";
import {
  ToolbarDropdownButton,
  ToolbarDropdownButtonChild
} from "../../../base/widgets/toolbar/model/ToolbarDropdownButton";
import { ToolbarButton } from "../../../base/widgets/toolbar/model/ToolbarButton";
import { LanguageModel } from "../../../base/components/model/LanguageModel";

export class MyCoReDesktopToolbarModel extends MyCoReBasicToolbarModel {

  constructor(name = "MyCoReDesktopToolbar") {
    super(name);
  }

  public _languageModel: LanguageModel;
  public viewSelectChilds: Array<ToolbarDropdownButtonChild>;
  public viewSelect: ToolbarDropdownButton;


  public selectionSwitchButton: ToolbarButton;

  public addComponents(): void {
    ;

    this.addGroup(this._sidebarControllGroup);
    this.addGroup(this._zoomControllGroup);
    this.addGroup(this._layoutControllGroup);
    this.addGroup(new ToolbarGroup("viewSelectGroup", 40));
    this.addGroup(this._imageChangeControllGroup);
    this.addGroup(this._actionControllGroup);
    this.addGroup(this._searchGroup);
    this.addGroup(this._closeViewerGroup);
  }


  public addViewSelectButton(): void {
    this.viewSelectChilds = new Array<ToolbarDropdownButtonChild>();

    this.viewSelectChilds.push({
      id: "imageView",
      label: "imageView"
    });

    this.viewSelectChilds.push({
      id: "mixedView",
      label: "mixedView"
    });

    this.viewSelectChilds.push({
      id: "textView",
      label: "textView"
    });

    this.viewSelect = new ToolbarDropdownButton('viewSelect', 'viewSelect', this.viewSelectChilds, 'eye');
    const vsg = this.getGroup("viewSelectGroup");
    if (vsg) {
      vsg.addComponent(this.viewSelect);
    }
  }

  public addSelectionSwitchButton(): void {
    this.selectionSwitchButton = new ToolbarButton("selectionSwitchButton", "", "text-select", "text-width");
    this.selectionSwitchButton.tooltip = this._languageModel.getTranslation('toolbar.textSelect');

    //this._actionControllGroup.addComponent(this.selectionSwitchButton);
  }

  public i18n(model: LanguageModel) {
    this._languageModel = model;
  }
}

