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


import { ToolbarModel } from "../../widgets/toolbar/model/ToolbarModel";
import { ToolbarButton } from "../../widgets/toolbar/model/ToolbarButton";
import { ToolbarGroup } from "../../widgets/toolbar/model/ToolbarGroup";
import { ToolbarDropdownButton } from "../../widgets/toolbar/model/ToolbarDropdownButton";
import { LanguageModel } from "./LanguageModel";

export class MyCoReBasicToolbarModel extends ToolbarModel {
  constructor(id: string) {
    super(id);
    this.initComponents();
    this.addComponents();
  }

  public _dropdownChildren = new Array<{
    id: string; label: string
  }>();

  public _imageOverviewDropdownChild;
  public _chapterOverviewDropdownChild;

  public _pageSelectChildren;

  public _sidebarControllGroup: ToolbarGroup;
  public _imageChangeControllGroup: ToolbarGroup;
  public _zoomControllGroup: ToolbarGroup;
  public _actionControllGroup: ToolbarGroup;
  public _layoutControllGroup: ToolbarGroup;
  public _searchGroup: ToolbarGroup;
  public _closeViewerGroup: ToolbarGroup;

  public _sidebarControllDropdownButton: ToolbarDropdownButton;
  public _previousImageButton: ToolbarButton;
  public _nextImageButton: ToolbarButton;
  public _zoomInButton: ToolbarButton;
  public _zoomOutButton: ToolbarButton;
  public _zoomWidthButton: ToolbarButton;
  public _zoomFitButton: ToolbarButton;
  public _shareButton: ToolbarButton;
  public _closeViewerButton: ToolbarButton;
  public _rotateButton: ToolbarButton;
  public _layoutDropdownButton: ToolbarDropdownButton;
  public _layoutDropdownButtonChilds;

  public _pageSelect;

  public initComponents(): void {
    // Dropdown Menu
    this._sidebarControllGroup = new ToolbarGroup('SidebarControllGroup', 10);
    this._sidebarControllDropdownButton = new ToolbarDropdownButton('SidebarControllDropdownButton', '', [], 'bars');

    this._imageOverviewDropdownChild = { id: 'imageOverview', label: 'Bildübersicht' };
    this._chapterOverviewDropdownChild = { id: 'chapterOverview', label: 'Strukturübersicht' };

    this._dropdownChildren.push(this._imageOverviewDropdownChild);
    this._dropdownChildren.push(this._chapterOverviewDropdownChild);

    this._sidebarControllDropdownButton.children = this._dropdownChildren;

    this._sidebarControllGroup.addComponent(this._sidebarControllDropdownButton);

    this._imageChangeControllGroup = new ToolbarGroup('ImageChangeControllGroup', 45);
    this._previousImageButton = new ToolbarButton('PreviousImageButton', '', 'previous-image', 'arrow-left');
    this._pageSelect = new ToolbarDropdownButton('PageSelect', '', [], null, true);

    this._pageSelectChildren = new Array<{
      id: string; label: string
    }>();
    this._pageSelect.children = this._pageSelectChildren;

    this._nextImageButton = new ToolbarButton('NextImageButton', '', 'next-image', 'arrow-right');

    this._imageChangeControllGroup.addComponent(this._previousImageButton);
    this._imageChangeControllGroup.addComponent(this._pageSelect);
    this._imageChangeControllGroup.addComponent(this._nextImageButton);

    // Zoom Group
    this._zoomControllGroup = new ToolbarGroup('ZoomControllGroup', 20);
    this._zoomInButton = new ToolbarButton('ZoomInButton', '', 'zoom-in', 'search-plus');
    this._zoomOutButton = new ToolbarButton('ZoomOutButton', '', 'zoom-out', 'search-minus');
    this._zoomWidthButton = new ToolbarButton('ZoomWidthButton', '', 'zoom-width', 'arrows-alt-h');
    this._zoomFitButton = new ToolbarButton('ZoomFitButton', '', 'zoom-fit-in', 'expand-arrows-alt');
    this._rotateButton = new ToolbarButton('RotateButton', '', 'Rotate', 'redo');

    this._zoomControllGroup.addComponent(this._zoomInButton);
    this._zoomControllGroup.addComponent(this._zoomOutButton);
    this._zoomControllGroup.addComponent(this._zoomWidthButton);
    this._zoomControllGroup.addComponent(this._zoomFitButton);
    this._zoomControllGroup.addComponent(this._rotateButton);

    this._layoutControllGroup = new ToolbarGroup('LayoutControllGroup', 30);
    this._layoutDropdownButtonChilds = [];
    this._layoutDropdownButton = new ToolbarDropdownButton('LayoutDropdownButton', '', this._layoutDropdownButtonChilds, 'book', false);
    this._layoutControllGroup.addComponent(this._layoutDropdownButton);


    this._actionControllGroup = new ToolbarGroup('ActionControllGroup', 60);
    this._shareButton = new ToolbarButton('ShareButton', '', 'share', 'share');
    this._actionControllGroup.addComponent(this._shareButton);

    this._searchGroup = new ToolbarGroup('SearchGroup', 80, true);


    this._closeViewerGroup = new ToolbarGroup('CloseViewerGroup', 100, true);
    this._closeViewerButton = new ToolbarButton('CloseViewerButton', '', 'close-viewer', 'power-off');
    this._closeViewerGroup.addComponent(this._closeViewerButton);
  }

  public addComponents(): void {
  }


  public i18n(model: LanguageModel) {
    this._previousImageButton.tooltip = model.getTranslation('toolbar.backward');
    this._nextImageButton.tooltip = model.getTranslation('toolbar.forward');
    this._zoomInButton.tooltip = model.getTranslation('toolbar.zoomIn');
    this._zoomOutButton.tooltip = model.getTranslation('toolbar.zoomOut');
    this._zoomWidthButton.tooltip = model.getTranslation('toolbar.toWidth');
    this._zoomFitButton.tooltip = model.getTranslation('toolbar.toScreen');
    this._shareButton.tooltip = model.getTranslation('toolbar.permalink');

    this._closeViewerButton.tooltip = model.getTranslation('toolbar.normalView');
    this._rotateButton.tooltip = model.getTranslation('toolbar.rotate');

    this._imageOverviewDropdownChild.label = model.getTranslation('toolbar.openThumbnailPanel');
    this._chapterOverviewDropdownChild.label = model.getTranslation('toolbar.openChapter');

    this._sidebarControllDropdownButton.children = this._sidebarControllDropdownButton.children;
  }


}

