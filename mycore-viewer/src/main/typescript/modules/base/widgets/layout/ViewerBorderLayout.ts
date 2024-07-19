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

import {MyCoReMap, Position2D} from "../../Utils";

/**
 * Border Layout designed for Desktop.
 * Every container can be resizable.
 */
export class ViewerBorderLayout {

    public static DIRECTION_CENTER = 0;
    public static DIRECTION_EAST = 1;
    public static DIRECTION_SOUTH = 2;
    public static DIRECTION_WEST = 3;
    public static DIRECTION_NORTH = 4;

    /**
     * Creates a new Border Layout.
     * @param _parent the Container in wich the layout will be applyed.
     * @param _horizontalStronger if true the East and West container get the full height
     * @param descriptions see ContainerDescription
     */
    constructor(private _parent: JQuery, private _horizontalStronger: boolean, descriptions: ContainerDescription[]) {

        this._containerMap = new MyCoReMap<number, JQuery>();
        this._descriptionMap = new MyCoReMap<number, ContainerDescription>();

        for (let i in descriptions) {
            const description: ContainerDescription = descriptions[i];
            this._descriptionMap.set(description.direction, description);
        }

        for (let i in descriptions) {
            const description: ContainerDescription = descriptions[i];
            this._initContainer(description);
        }

        window.onresize =  () => {
            this.updateSizes();
        };

        this._initCenter();
    }


    private _initCenter() {
        var centerContainerDiv = jQuery("<div></div>");
        centerContainerDiv.addClass(this.getDirectionDescription(ViewerBorderLayout.DIRECTION_CENTER));
        var cssDescription = this._updateCenterCss();
        this._parent.append(centerContainerDiv);
        centerContainerDiv.css(cssDescription);
        this._containerMap.set(ViewerBorderLayout.DIRECTION_CENTER, centerContainerDiv);
    }

    private _updateCenterCss(): any {
        var cssDescription = <any>{};
        cssDescription.position = "absolute";
        cssDescription.left = this.getContainerSize(ViewerBorderLayout.DIRECTION_WEST) + "px";
        cssDescription.top = this.getContainerSize(ViewerBorderLayout.DIRECTION_NORTH) + "px";
        cssDescription.bottom = this.getContainerSize(ViewerBorderLayout.DIRECTION_SOUTH) + "px";
        cssDescription.right = this.getContainerSize(ViewerBorderLayout.DIRECTION_EAST) + "px";
        return cssDescription;
    }

    private _initContainer(description: ContainerDescription) {
        var containerDiv = jQuery("<div></div>");

        containerDiv.addClass(this.getDirectionDescription(description.direction));

        if (typeof description.resizeable == "undefined") {
            description.resizeable = false;
        }

        if (description.resizeable) {
            this._initContainerResizeable(containerDiv, description);
        }

        this._correctDescription(description);
        var cssDescription = this._updateCssDescription(description);

        containerDiv.css(cssDescription);
        this._parent.append(containerDiv);
        this._containerMap.set(description.direction, containerDiv);
    }

    private _correctDescription(description: ContainerDescription) {
        if ("minSize" in description && !isNaN(description.minSize)) {
            var minimumSize = description.minSize;

            description.size = Math.max(description.minSize, description.size);
        }
    }

    private _updateCssDescription(description: ContainerDescription): any {
        var cssDescription = <any>{};
        cssDescription.position = "absolute";

        cssDescription.right = "0px";
        cssDescription.top = "0px";
        cssDescription.bottom = "0px";
        cssDescription.left = "0px";
        cssDescription.display = description.size !== 0 ? "block" : "none";

        switch (description.direction) {
            case ViewerBorderLayout.DIRECTION_EAST:
                delete cssDescription.left;
                break;
            case ViewerBorderLayout.DIRECTION_WEST:
                delete cssDescription.right;
                break;
            case ViewerBorderLayout.DIRECTION_SOUTH:
                delete cssDescription.top;
                break;
            case ViewerBorderLayout.DIRECTION_NORTH:
                delete cssDescription.bottom;
                break;
        }

        if (description.direction == ViewerBorderLayout.DIRECTION_NORTH || description.direction == ViewerBorderLayout.DIRECTION_SOUTH) {
            if (this.hasContainer(ViewerBorderLayout.DIRECTION_WEST) && this.horizontalStronger) {
                cssDescription.left = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_WEST) + "px";
            }
            if (this.hasContainer(ViewerBorderLayout.DIRECTION_EAST) && this.horizontalStronger) {
                cssDescription.right = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_EAST) + "px";
            }
            cssDescription.height = description.size + "px";
        } else {
            if (this.hasContainer(ViewerBorderLayout.DIRECTION_NORTH) && !this.horizontalStronger) {
                cssDescription.top = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_NORTH) + "px";
            }
            if (this.hasContainer(ViewerBorderLayout.DIRECTION_SOUTH) && !this.horizontalStronger) {
                cssDescription.bottom = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_SOUTH) + "px";
            }
            cssDescription.width = description.size + "px";
        }


        return cssDescription;
    }

    public updateSizes() {
        var descriptions = this._descriptionMap.values;
        for (var i in descriptions) {
            var description: ContainerDescription = descriptions[i];
            var container = this._containerMap.get(description.direction);
            this._correctDescription(description);
            container.css(this._updateCssDescription(description));
            container.delay(10).children().trigger("iviewResize");
        }

        var container = this._containerMap.get(ViewerBorderLayout.DIRECTION_CENTER);
        container.css(this._updateCenterCss());
        container.delay(10).children().trigger("iviewResize")
    }

    private _initContainerResizeable(containerDiv: JQuery, description: ContainerDescription) {
        var resizerElement = jQuery("<span></span>");

        resizerElement.addClass("resizer");
        var that = this;
        resizerElement.bind("mousedown", function resizerMouseDown(e: JQuery.MouseDownEvent) {
            var startPos = new Position2D(e.clientX, e.clientY);
            var startSize = description.size;

            var MOUSE_MOVE = function (e: JQuery.MouseEventBase) {
                var curPos = new Position2D(e.clientX, e.clientY);

                description.size = that._getNewSize(startPos, curPos, startSize, description.direction);
                e.preventDefault();
                that.updateSizes();
            };

            var MOUSE_UP = function (e: JQuery.MouseEventBase) {
                var curPos = new Position2D(e.clientX, e.clientY);
                that._parent.unbind("mousemove");
                that._parent.unbind("mouseup");
                // trigger resize in this and center
            };

            e.preventDefault();
            jQuery(that._parent).bind("mousemove", MOUSE_MOVE);
            jQuery(that._parent).bind("mouseup", MOUSE_UP);
        });


        var cssElem = <any>{};
        cssElem.position = "absolute";
        var resizeWidth = 6;

        if (description.direction == ViewerBorderLayout.DIRECTION_NORTH || ViewerBorderLayout.DIRECTION_SOUTH == description.direction) {
            cssElem.cursor = "row-resize";
            cssElem.left = "0px";
            cssElem.height = resizeWidth + "px";
            cssElem.right = "0px";
            if (description.direction == ViewerBorderLayout.DIRECTION_NORTH) {
                cssElem.bottom = -(resizeWidth / 2) + "px";
            } else {
                cssElem.top = -(resizeWidth / 2) + "px";
            }
        }

        if (description.direction == ViewerBorderLayout.DIRECTION_WEST || ViewerBorderLayout.DIRECTION_EAST == description.direction) {
            cssElem.cursor = "col-resize";
            cssElem.top = "0px";
            cssElem.bottom = "0px";
            cssElem.width = resizeWidth + "px";
            if (description.direction == ViewerBorderLayout.DIRECTION_WEST) {
                cssElem.right = -(resizeWidth / 2) + "px";
            } else {
                cssElem.left = -(resizeWidth / 2) + "px";
            }
        }


        resizerElement.css(cssElem);
        resizerElement.appendTo(containerDiv);

    }

    private _descriptionMap: MyCoReMap<number, ContainerDescription>;
    private _containerMap: MyCoReMap<number, JQuery>;

    private _getNewSize(startPosition: Position2D, currentPosition: Position2D, startSize: number, direction: number): number {
        var newSize;
        if (direction == ViewerBorderLayout.DIRECTION_EAST || direction == ViewerBorderLayout.DIRECTION_WEST) {
            var diff = startPosition.x - currentPosition.x;
            if (direction == ViewerBorderLayout.DIRECTION_EAST) {
                newSize = startSize + diff;
            } else {
                newSize = startSize - diff;
            }
        }
        if (direction == ViewerBorderLayout.DIRECTION_NORTH || direction == ViewerBorderLayout.DIRECTION_SOUTH) {
            var diff = startPosition.y - currentPosition.y;
            if (direction == ViewerBorderLayout.DIRECTION_SOUTH) {
                newSize = startSize + diff;
            } else {
                newSize = startSize - diff;
            }
        }

        return newSize;
    }

    public hasContainer(direction: number) {
        return this._descriptionMap.has(direction) || this._containerMap.has(direction);
    }

    public getContainer(direction: number) {
        if (this._containerMap.has(direction)) {
            return this._containerMap.get(direction);

        } else {
            return null;
        }
    }


    public get horizontalStronger() {
        return this._horizontalStronger;
    }


    public getContainerSizeDescription(direction: number) {
        return this._descriptionMap.get(direction).size;
    }

    public getContainerDescription(direction: number) {
        return this._descriptionMap.get(direction);
    }

    public getContainerSize(direction: number) {
        if (this.hasContainer(direction)) {
            var container: JQuery = this.getContainer(direction);
            if (direction == ViewerBorderLayout.DIRECTION_EAST || direction == ViewerBorderLayout.DIRECTION_WEST) {
                return container.width();

            } else if (direction == ViewerBorderLayout.DIRECTION_NORTH || direction == ViewerBorderLayout.DIRECTION_SOUTH) {
                return container.height();

            } else {
                // its center..
                return container.width();
            }

        } else {
            return 0;
        }
    }

    private getDirectionDescription(direction: number): string {
        return ["center", "east", "south", "west", "north"][direction];
    }
}

/**
 * The Description for containers in Border Layout
 */
export interface ContainerDescription {
    /**
     * IviewBorderLayout.DIRECTION_EAST
     * IviewBorderLayout.DIRECTION_SOUTH
     * IviewBorderLayout.DIRECTION_WEST
     * IviewBorderLayout.DIRECTION_NORTH
     */
    direction: number;

    /**
     * should the container rezisable ? [Default: false]
     */
    resizeable?: boolean;

    /**
     * The initial Size in Pixel
     */
    size: number;

    /**
     *  The minimum size in Pixel
     */
    minSize?: number;
}


