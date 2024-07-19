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


import {ViewerComponent} from "./ViewerComponent";
import {MyCoReViewerSettings} from "../MyCoReViewerSettings";
import {ContainerDescription, ViewerBorderLayout} from "../widgets/layout/ViewerBorderLayout";
import {MyCoReMap} from "../Utils";
import {ViewerEvent} from "../widgets/events/ViewerEvent";
import {ComponentInitializedEvent} from "./events/ComponentInitializedEvent";
import {ShowContentEvent} from "./events/ShowContentEvent";
import {DropdownButtonPressedEvent} from "../widgets/toolbar/events/DropdownButtonPressedEvent";
import {ButtonPressedEvent} from "../widgets/toolbar/events/ButtonPressedEvent";
import {ImageSelectedEvent} from "./events/ImageSelectedEvent";


export class MyCoReViewerContainerComponent extends ViewerComponent {

    constructor(private _settings: MyCoReViewerSettings, private _container: JQuery, private _contentContainer = jQuery("<div></div>")) {
        super();
    }

    public init() {
        this._container.append(this._contentContainer);

        this._container.css({"overflow": "hidden"});

        jQuery(this._contentContainer).css({
            "left": "0px",
            "bottom": "0px",
            "right": "0px",
            "position": "absolute"
        });

        let containerDescriptions: ContainerDescription[] = [];

        if (!this._settings.mobile) {
            containerDescriptions = [
                {direction: MyCoReViewerContainerComponent.SIDEBAR_DIRECTION, resizeable: true, size: 0, minSize: 0},
                {direction: MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION, resizeable: false, size: 30},
                {direction: ViewerBorderLayout.DIRECTION_EAST, resizeable: true, size: 0}
            ];
        }

        this._layout = new ViewerBorderLayout(this._contentContainer, false, containerDescriptions);
        this._content = this._layout.getContainer(MyCoReViewerContainerComponent.CONTENT_DIRECTION);

        if (!this._settings.mobile) {
            this._sidebar = this._layout.getContainer(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION);
            this._informationBar = this._layout.getContainer(MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION);
            this._sidebar.addClass("card sidebar");

            this._informationBar = this._layout.getContainer(MyCoReViewerContainerComponent.INFORMATION_BAR_DIRECTION);
            this._layout.getContainer(ViewerBorderLayout.DIRECTION_EAST).addClass("card sidebar");
            this._container.bind("iviewResize", () => {
                this.correctToToolbarSize();
            });
        }
    }

    private correctToToolbarSize() {
        const toolbar = this._container.parent().find(".navbar.navbar-light");
        const heightOfToolbar = toolbar.outerHeight(false);
        toolbar.siblings().css({"top": heightOfToolbar + "px"});
    }

    private static SIDEBAR_DIRECTION = ViewerBorderLayout.DIRECTION_WEST;
    private static CONTENT_DIRECTION = ViewerBorderLayout.DIRECTION_CENTER;
    private static INFORMATION_BAR_DIRECTION = ViewerBorderLayout.DIRECTION_SOUTH;

    private _sidebar: JQuery;
    private _content: JQuery;
    private _informationBar: JQuery;
    private _layout: ViewerBorderLayout;
    private _lastSizeMap = new MyCoReMap<number, number>();

    public handle(e: ViewerEvent) {
        if (e.type == ComponentInitializedEvent.TYPE) {

        }

        if (e.type == ShowContentEvent.TYPE) {
            const sce = e as ShowContentEvent;
            const container = this._layout.getContainer(sce.containerDirection);
            this._clearOldContent(container);


            container.append(sce.content);


            if (sce.text != null && !this._settings.mobile) {
                const header = jQuery("<div></div>");
                header.addClass("card-header");

                const closeButton = jQuery("<button></button>");
                closeButton.attr("type", "button");
                closeButton.addClass("close");

                const inSpan = jQuery("<span aria-hidden=\"true\">&times;</span><span class=\"sr-only\">Close</span>")
                closeButton.append(inSpan);
                header.prepend(sce.text);
                header.append(closeButton);

                container.prepend(header);

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

            this.correctToToolbarSize();
            return;
        }

        if (e.type == DropdownButtonPressedEvent.TYPE) {
            const dropdownButtonPressedEvent = e as DropdownButtonPressedEvent;
            if (dropdownButtonPressedEvent.childId == "close") {
                this._closeSidebar();
            }
            return;
        }

        if (e.type == ButtonPressedEvent.TYPE) {
            const buttonPressedEvent = e as ButtonPressedEvent;
            if (buttonPressedEvent.button.id == "CloseViewerButton") {
                if (typeof this._settings.onClose !== "function") {
                    if (window.history.length > 1) {
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
        const description = this._layout.getContainerDescription(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION);
        description.size = 0;
        description.minSize = 0;
        this._clearOldContent(this._layout.getContainer(MyCoReViewerContainerComponent.SIDEBAR_DIRECTION));
        this._layout.updateSizes();
    }

    private _clearOldContent(container: JQuery) {
        container.children().not(".resizer").detach();
    }


    public get handlesEvents(): string[] {
        return [ButtonPressedEvent.TYPE, ShowContentEvent.TYPE, ImageSelectedEvent.TYPE]
    }


}

