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

/// <reference path="../definitions/jquery.d.ts" />
/// <reference path="../Utils.ts" />
/// <reference path="ViewerComponent.ts" />
/// <reference path="../MyCoReViewerSettings.ts" />
/// <reference path="../widgets/layout/IviewBorderLayout.ts" />
/// <reference path="../widgets/toolbar/events/ButtonPressedEvent.ts" />
/// <reference path="../widgets/toolbar/events/DropdownButtonPressedEvent.ts" />
/// <reference path="../widgets/events/ViewerEvent.ts" />
/// <reference path="events/ComponentInitializedEvent.ts" />
/// <reference path="events/ImageChangedEvent.ts" />
/// <reference path="events/ImageSelectedEvent.ts" />
/// <reference path="events/ShowContentEvent.ts" />

namespace mycore.viewer.components {
    export class MyCoReViewerContainerComponent extends ViewerComponent {

        constructor(private _settings: MyCoReViewerSettings, private _container: JQuery, private _contentContainer = jQuery("<div></div>")) {
            super();
        }

        public init() {
            this._container.append(this._contentContainer);

            this._container.css({"overflow" : "hidden"});

            jQuery(this._contentContainer).css({
                "left": "0px",
                "bottom": "0px",
                "right": "0px",
                "position": "absolute"
            });

            var containerDescriptions: widgets.layout.ContainerDescription[] = [];

            if (!this._settings.mobile) {
                containerDescriptions = [
                    { direction: MyCoReViewerContainerComponent.SIDEBAR_DIRECTION, resizeable: true, size: 0, minSize: 0 },
                    { direction: MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION, resizeable: false, size: 30 },
                    { direction: mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_EAST, resizeable: true, size: 0 }
                ];
            }

            this._layout = new mycore.viewer.widgets.layout.IviewBorderLayout(this._contentContainer, false, containerDescriptions);
            this._content = this._layout.getContainer(MyCoReViewerContainerComponent.CONTENT_DIRECTION);

            if (!this._settings.mobile) {
                this._sidebar = this._layout.getContainer(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION);
                this._informationBar = this._layout.getContainer(MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION);
                this._sidebar.addClass("panel panel-default sidebar");

                this._informationBar = this._layout.getContainer(MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION);
                this._layout.getContainer(widgets.layout.IviewBorderLayout.DIRECTION_EAST).addClass("panel panel-default sidebar");
            }
        }

        private static SIDEBAR_DIRECTION = mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST;
        private static CONTENT_DIRECTION = mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_CENTER;
        private static INFORMATION_BAR_DIRECTION = mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_SOUTH;

        private _sidebar: JQuery;
        private _content: JQuery;
        private _informationBar: JQuery;
        private _layout: widgets.layout.IviewBorderLayout;
        private _lastSizeMap = new MyCoReMap<number, number>();

        public handle(e: mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.ComponentInitializedEvent.TYPE) {

            }

            if (e.type == events.ShowContentEvent.TYPE) {
                var sce = <events.ShowContentEvent> e;
                var container = this._layout.getContainer(sce.containerDirection);
                this._clearOldContent(container);


                container.append(sce.content);


                if (sce.text != null && !this._settings.mobile) {
                    var heading = jQuery("<div></div>");
                    heading.addClass("panel-heading");

                    var closeButton = jQuery("<button></button>");
                    closeButton.attr("type", "button");
                    closeButton.addClass("close");

                    var inSpan = jQuery("<span aria-hidden=\"true\">&times;</span><span class=\"sr-only\">Close</span>")
                    closeButton.append(inSpan);
                    heading.prepend(sce.text);
                    heading.append(closeButton);

                    container.prepend(heading);

                    closeButton.click(() => {
                        sce.component = this;
                        sce.size = 0;
                        this.trigger(sce);
                    });
                }

                if (sce.containerDirection != MyCoReViewerContainerComponent.CONTENT_DIRECTION) {
                    var containerDescription = this._layout.getContainerDescription(sce.containerDirection);
                    if (sce.size != -1) {
                        if (sce.size == 0) {
                            container.css({display: "none"});
                            this._lastSizeMap.set(containerDescription.direction, containerDescription.size);
                            containerDescription.size = 0;
                        } else {
                            container.css({display: "block"});
                            containerDescription.size = sce.size;
                        }

                    } else {
                        container.css({display: "block"});
                        if (containerDescription.size == 0 && this._lastSizeMap.has(containerDescription.direction)) {
                            containerDescription.size = this._lastSizeMap.get(containerDescription.direction);
                        } else if (containerDescription.size > 0) {
                            containerDescription.size = containerDescription.size;
                        } else {
                            containerDescription.size = 300;
                        }
                    }
                    this._layout.updateSizes();
                }
            }

            if (e.type == mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent.TYPE) {
                var dropdownButtonPressedEvent = <mycore.viewer.widgets.toolbar.events.DropdownButtonPressedEvent> e;
                if (dropdownButtonPressedEvent.childId == "close") {
                    this._closeSidebar();
                }
            }

            if (e.type == mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var buttonPressedEvent = <mycore.viewer.widgets.toolbar.events.ButtonPressedEvent> e;
                if (buttonPressedEvent.button.id == "CloseViewerButton") {
                    if (typeof this._settings.onClose !== "function" || this._settings.onClose == null) {
                        if(window.history.length>1){
                            window.history.back();
                        } else {
                            window.close();
                        }
                    } else {
                        this._settings.onClose.apply(window);
                    }
                }
            }

        }

        private _closeSidebar() {
            var description = this._layout.getContainerDescription(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION);
            description.size = 0;
            description.minSize = 0;
            this._clearOldContent(this._layout.getContainer(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION));
            this._layout.updateSizes();
        }

        private _clearOldContent(container: JQuery) {
            container.children().not(".resizer").detach();
        }


        public get handlesEvents(): string[] {
            return [mycore.viewer.widgets.toolbar.events.ButtonPressedEvent.TYPE,
                mycore.viewer.components.events.ShowContentEvent.TYPE,
                events.ImageSelectedEvent.TYPE]
        }


    }
}
