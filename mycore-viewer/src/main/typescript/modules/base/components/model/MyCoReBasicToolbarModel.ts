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

/// <reference path="../../widgets/toolbar/model/ToolbarModel.ts" />
/// <reference path="../../widgets/toolbar/model/ToolbarGroup.ts" />
/// <reference path="../../widgets/toolbar/model/ToolbarButton.ts" />
/// <reference path="../../widgets/toolbar/model/ToolbarDropdownButton.ts" />
/// <reference path="LanguageModel.ts" />

namespace mycore.viewer.model {
    import ToolbarButton = mycore.viewer.widgets.toolbar.ToolbarButton;
    import ToolbarDropdownButton = mycore.viewer.widgets.toolbar.ToolbarDropdownButton;

    export class MyCoReBasicToolbarModel extends widgets.toolbar.ToolbarModel {
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

        public _sidebarControllGroup:widgets.toolbar.ToolbarGroup;
        public _imageChangeControllGroup:widgets.toolbar.ToolbarGroup;
        public _zoomControllGroup:widgets.toolbar.ToolbarGroup;
        public _actionControllGroup:widgets.toolbar.ToolbarGroup;
        public _layoutControllGroup:widgets.toolbar.ToolbarGroup;
        public _searchGroup:widgets.toolbar.ToolbarGroup;
        public _closeViewerGroup:widgets.toolbar.ToolbarGroup;

        public _sidebarControllDropdownButton: widgets.toolbar.ToolbarDropdownButton;
        public _previousImageButton:ToolbarButton;
        public _nextImageButton:ToolbarButton;
        public _zoomInButton:ToolbarButton;
        public _zoomOutButton:ToolbarButton;
        public _zoomWidthButton:ToolbarButton;
        public _zoomFitButton:ToolbarButton;
        public _shareButton:ToolbarButton;
        public _closeViewerButton:ToolbarButton;
        public _rotateButton:ToolbarButton;
        public _layoutDropdownButton:ToolbarDropdownButton;
        public _layoutDropdownButtonChilds;

        public _pageSelect;

        public initComponents(): void {
            // Dropdown Menu
            this._sidebarControllGroup = new widgets.toolbar.ToolbarGroup("SidebarControllGroup");
            this._sidebarControllDropdownButton = new widgets.toolbar.ToolbarDropdownButton("SidebarControllDropdownButton", "", [], "menu-hamburger");

            this._imageOverviewDropdownChild = { id: "imageOverview", label: "Bildübersicht" };
            this._chapterOverviewDropdownChild = { id: "chapterOverview", label: "Strukturübersicht" };

            this._dropdownChildren.push(this._imageOverviewDropdownChild);
            this._dropdownChildren.push(this._chapterOverviewDropdownChild);

            this._sidebarControllDropdownButton.children = this._dropdownChildren;

            this._sidebarControllGroup.addComponent(this._sidebarControllDropdownButton);

            this._imageChangeControllGroup = new widgets.toolbar.ToolbarGroup("ImageChangeControllGroup");
            this._previousImageButton = new widgets.toolbar.ToolbarButton("PreviousImageButton", "", "previous-image", "arrow-left");
            this._pageSelect = new widgets.toolbar.ToolbarDropdownButton("PageSelect", "", [], null, true);

            this._pageSelectChildren = new Array<{
                id: string; label: string
            }>();
            this._pageSelect.children = this._pageSelectChildren;

            this._nextImageButton = new widgets.toolbar.ToolbarButton("NextImageButton", "", "next-image", "arrow-right");

            this._imageChangeControllGroup.addComponent(this._previousImageButton);
            this._imageChangeControllGroup.addComponent(this._pageSelect);
            this._imageChangeControllGroup.addComponent(this._nextImageButton);

            // Zoom Group
            this._zoomControllGroup = new widgets.toolbar.ToolbarGroup("ZoomControllGroup");
            this._zoomInButton = new widgets.toolbar.ToolbarButton("ZoomInButton", "", "zoom-in", "plus");
            this._zoomOutButton = new widgets.toolbar.ToolbarButton("ZoomOutButton", "", "zoom-out", "minus");
            this._zoomWidthButton = new widgets.toolbar.ToolbarButton("ZoomWidthButton", "", "zoom-width", "resize-horizontal");
            this._zoomFitButton = new widgets.toolbar.ToolbarButton("ZoomFitButton", "", "zoom-fit-in", "resize-full");
            this._rotateButton = new widgets.toolbar.ToolbarButton("RotateButton", "", "Rotate", "repeat");

            this._zoomControllGroup.addComponent(this._zoomInButton);
            this._zoomControllGroup.addComponent(this._zoomOutButton);
            this._zoomControllGroup.addComponent(this._zoomWidthButton);
            this._zoomControllGroup.addComponent(this._zoomFitButton);
            this._zoomControllGroup.addComponent(this._rotateButton);

            this._layoutControllGroup = new widgets.toolbar.ToolbarGroup("LayoutControllGroup");
            this._layoutDropdownButtonChilds = [
            ];
            this._layoutDropdownButton = new widgets.toolbar.ToolbarDropdownButton("LayoutDropdownButton", "", this._layoutDropdownButtonChilds, "book", false);
            this._layoutControllGroup.addComponent(this._layoutDropdownButton);
            



            this._actionControllGroup = new widgets.toolbar.ToolbarGroup("ActionControllGroup");
            this._shareButton = new widgets.toolbar.ToolbarButton("ShareButton", "", "share", "share");
            this._actionControllGroup.addComponent(this._shareButton);

            this._searchGroup = new widgets.toolbar.ToolbarGroup("SearchGroup", true);


            this._closeViewerGroup = new widgets.toolbar.ToolbarGroup("CloseViewerGroup", true);
            this._closeViewerButton = new widgets.toolbar.ToolbarButton("CloseViewerButton", "", "close-viewer", "off");
            this._closeViewerGroup.addComponent(this._closeViewerButton);
        }

        public addComponents(): void {
        }


        public i18n(model: model.LanguageModel) {
            this._previousImageButton.tooltip = model.getTranslation("toolbar.backward");
            this._nextImageButton.tooltip = model.getTranslation("toolbar.forward");
            this._zoomInButton.tooltip = model.getTranslation("toolbar.zoomIn");
            this._zoomOutButton.tooltip = model.getTranslation("toolbar.zoomOut");
            this._zoomWidthButton.tooltip = model.getTranslation("toolbar.toWidth");
            this._zoomFitButton.tooltip = model.getTranslation("toolbar.toScreen");
            this._shareButton.tooltip = model.getTranslation("toolbar.permalink");

            this._closeViewerButton.tooltip = model.getTranslation("toolbar.normalView");
            this._rotateButton.tooltip = model.getTranslation("toolbar.rotate");

            this._imageOverviewDropdownChild.label = model.getTranslation("toolbar.openThumbnailPanel");
            this._chapterOverviewDropdownChild.label = model.getTranslation("toolbar.openChapter");

            this._sidebarControllDropdownButton.children = this._sidebarControllDropdownButton.children;
        }


    }
}
