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

import {getElementHeight, getElementWidth, MyCoReMap, Position2D} from "../../Utils";

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
   * @param _parent the Container in wich the layout will be applied.
   * @param _horizontalStronger if true the East and West container get the full height
   * @param descriptions see ContainerDescription
   */
  constructor(private _parent: HTMLElement, private _horizontalStronger: boolean, descriptions: ContainerDescription[]) {

    this._containerMap = new MyCoReMap<number, HTMLElement>();
    this._descriptionMap = new MyCoReMap<number, ContainerDescription>();

    for (let i in descriptions) {
      const description: ContainerDescription = descriptions[i];
      this._descriptionMap.set(description.direction, description);
    }

    for (let i in descriptions) {
      const description: ContainerDescription = descriptions[i];
      this._initContainer(description);
    }

    window.onresize = () => {
      this.updateSizes();
    };

    this._initCenter();
  }


  private _initCenter() {
    const centerContainerDiv = document.createElement("div");
    centerContainerDiv.classList.add(this.getDirectionString(ViewerBorderLayout.DIRECTION_CENTER));
    this._updateCenterCss(centerContainerDiv.style);
    this._parent.append(centerContainerDiv);
    this._containerMap.set(ViewerBorderLayout.DIRECTION_CENTER, centerContainerDiv);
  }

  private _updateCenterCss(style:CSSStyleDeclaration) {
    style.position = "absolute";
    style.left = this.getContainerSize(ViewerBorderLayout.DIRECTION_WEST) + "px";
    style.top = this.getContainerSize(ViewerBorderLayout.DIRECTION_NORTH) + "px";
    style.bottom = this.getContainerSize(ViewerBorderLayout.DIRECTION_SOUTH) + "px";
    style.right = this.getContainerSize(ViewerBorderLayout.DIRECTION_EAST) + "px";
  }

  private _initContainer(description: ContainerDescription) {
    const containerDiv = document.createElement("div");

    const direction = this.getDirectionString(description.direction);
    containerDiv.classList.add(direction);

    if (typeof description.resizeable == "undefined") {
      description.resizeable = false;
    }

    if (description.resizeable) {
      this._initContainerResizeable(containerDiv, description);
    }

    this._correctDescription(description);
    this._updateCssDescription(description,containerDiv.style);

    this._parent.append(containerDiv);
    this._containerMap.set(description.direction, containerDiv);
  }

  private _correctDescription(description: ContainerDescription) {
    if ("minSize" in description && !isNaN(description.minSize)) {
      description.size = Math.max(description.minSize, description.size);
    }
  }

  private _updateCssDescription(description: ContainerDescription, style: CSSStyleDeclaration) {
    style.position = "absolute";

    style.right = "0px";
    style.top = "0px";
    style.bottom = "0px";
    style.left = "0px";
    style.display = description.size !== 0 ? "block" : "none";

    switch (description.direction) {
      case ViewerBorderLayout.DIRECTION_EAST:
        style.left = "";
        break;
      case ViewerBorderLayout.DIRECTION_WEST:
        style.right = "";
        break;
      case ViewerBorderLayout.DIRECTION_SOUTH:
        style.top = "";
        break;
      case ViewerBorderLayout.DIRECTION_NORTH:
        style.bottom = "";
        break;
    }

    if (description.direction == ViewerBorderLayout.DIRECTION_NORTH || description.direction == ViewerBorderLayout.DIRECTION_SOUTH) {
      if (this.hasContainer(ViewerBorderLayout.DIRECTION_WEST) && this.horizontalStronger) {
        style.left = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_WEST) + "px";
      }
      if (this.hasContainer(ViewerBorderLayout.DIRECTION_EAST) && this.horizontalStronger) {
        style.right = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_EAST) + "px";
      }
      style.height = description.size + "px";
    } else {
      if (this.hasContainer(ViewerBorderLayout.DIRECTION_NORTH) && !this.horizontalStronger) {
        style.top = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_NORTH) + "px";
      }
      if (this.hasContainer(ViewerBorderLayout.DIRECTION_SOUTH) && !this.horizontalStronger) {
        style.bottom = this.getContainerSizeDescription(ViewerBorderLayout.DIRECTION_SOUTH) + "px";
      }
      style.width = description.size + "px";
    }

  }

  public updateSizes() {
    const descriptions = this._descriptionMap.values;
    for (const description of descriptions) {
      const container = this._containerMap.get(description.direction);
      this._correctDescription(description);
      this._updateCssDescription(description, container.style);
      window.setTimeout(() => {
        Array.from(container.children).forEach(child => {
          child.dispatchEvent(new Event("iviewResize"));
        });
      }, 10);
    }

    const container = this._containerMap.get(ViewerBorderLayout.DIRECTION_CENTER);
    this._updateCenterCss(container.style);
    window.setTimeout(() => {
      Array.from(container.children).forEach(child => {
        child.dispatchEvent(new Event("iviewResize"));
      });
    }, 10);
  }

  private _initContainerResizeable(containerDiv: HTMLElement, description: ContainerDescription) {
    const resizerElement = document.createElement("span");

    resizerElement.classList.add("resizer");
    resizerElement.addEventListener("mousedown", (e: MouseEvent) => {
      const startPos = new Position2D(e.clientX, e.clientY);
      const startSize = description.size;

      const MOUSE_MOVE = (e: MouseEvent) => {
        const curPos = new Position2D(e.clientX, e.clientY);

        description.size = this._getNewSize(startPos, curPos, startSize, description.direction);
        e.preventDefault();
        this.updateSizes();
      };

      const MOUSE_UP = (e: MouseEvent) => {
        const curPos = new Position2D(e.clientX, e.clientY);
        this._parent.removeEventListener("mousemove", MOUSE_MOVE);
        this._parent.removeEventListener("mouseup", MOUSE_UP);
        // trigger resize in this and center
      };

      e.preventDefault();
      this._parent.addEventListener("mousemove", MOUSE_MOVE);
      this._parent.addEventListener("mouseup", MOUSE_UP);
    });


    const cssStyleList = resizerElement.style;
    cssStyleList.position = "absolute";
    const resizeWidth = 6;

    if (description.direction == ViewerBorderLayout.DIRECTION_NORTH || ViewerBorderLayout.DIRECTION_SOUTH == description.direction) {
      cssStyleList.cursor = "row-resize";
      cssStyleList.left = "0px";
      cssStyleList.height = resizeWidth + "px";
      cssStyleList.right = "0px";
      if (description.direction == ViewerBorderLayout.DIRECTION_NORTH) {
        cssStyleList.bottom = -(resizeWidth / 2) + "px";
      } else {
        cssStyleList.top = -(resizeWidth / 2) + "px";
      }
    }

    if (description.direction == ViewerBorderLayout.DIRECTION_WEST || ViewerBorderLayout.DIRECTION_EAST == description.direction) {
      cssStyleList.cursor = "col-resize";
      cssStyleList.top = "0px";
      cssStyleList.bottom = "0px";
      cssStyleList.width = resizeWidth + "px";
      if (description.direction == ViewerBorderLayout.DIRECTION_WEST) {
        cssStyleList.right = -(resizeWidth / 2) + "px";
      } else {
        cssStyleList.left = -(resizeWidth / 2) + "px";
      }
    }

    containerDiv.append(resizerElement);
  }

  private _descriptionMap: MyCoReMap<number, ContainerDescription>;
  private _containerMap: MyCoReMap<number, HTMLElement>;

  private _getNewSize(startPosition: Position2D, currentPosition: Position2D, startSize: number, direction: number): number {
    let newSize: number;
    if (direction == ViewerBorderLayout.DIRECTION_EAST || direction == ViewerBorderLayout.DIRECTION_WEST) {
      const diff = startPosition.x - currentPosition.x;
      if (direction == ViewerBorderLayout.DIRECTION_EAST) {
        newSize = startSize + diff;
      } else {
        newSize = startSize - diff;
      }
    }
    if (direction == ViewerBorderLayout.DIRECTION_NORTH || direction == ViewerBorderLayout.DIRECTION_SOUTH) {
      const diff = startPosition.y - currentPosition.y;
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
      const container: HTMLElement = this.getContainer(direction);
      if (direction == ViewerBorderLayout.DIRECTION_EAST || direction == ViewerBorderLayout.DIRECTION_WEST) {
        return getElementWidth(container);
      } else if (direction == ViewerBorderLayout.DIRECTION_NORTH || direction == ViewerBorderLayout.DIRECTION_SOUTH) {
        return getElementHeight(container);
      } else {
        return getElementWidth(container);
      }

    } else {
      return 0;
    }
  }

  private getDirectionString(direction: number): string {
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


