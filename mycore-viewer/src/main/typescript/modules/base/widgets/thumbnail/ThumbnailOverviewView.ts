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

import { ThumbnailOverviewInputHandler } from "./ThumbnailOverviewInputHandler";
import {getElementHeight, getElementWidth, Position2D, Size2D} from "../../Utils";

export class ThumbnailOverviewView {
  constructor(private _container: HTMLElement,
    private _scrollHandler: ThumbnailOverviewScrollHandler,
    private _resizeHandler: ThumbnailOverviewResizeHandler,
    private _inputHandler: ThumbnailOverviewInputHandler) {
    this._gap = 0;
    this._spacer = document.createElement("div");
    this._container.append(this._spacer)


    const cssObj = this._container.style;
    cssObj.position = "relative";
    cssObj.overflowY = "scroll";
    cssObj.overflowX = "hidden";

    this._lastViewPortSize = this.getViewportSize();
    const scrollHandler = () => {
      const newPos = new Position2D(this._container.scrollLeft, this._container.scrollTop);
      this._scrollHandler.scrolled(newPos);
    };

    // TODO: Use touch
    this._container.addEventListener("scroll", scrollHandler);


    const resizeHandler = () => {
      const newVp = this.getViewportSize();
      if (this._lastViewPortSize != newVp) {
        this._resizeHandler.resized(newVp);
        this._lastViewPortSize = this.getViewportSize();
        scrollHandler();
      }
    }

    this._container.addEventListener("iviewResize", resizeHandler);

  }

  private _gap: number;
  private _lastViewPortSize: Size2D;
  private _spacer: HTMLElement;

  public set gap(num: number) {
    this._gap = num;
  }

  public get gap() {
    return this._gap;
  }

  public setContainerSize(newContainerSize: Size2D) {
    this._spacer.style.width = newContainerSize.width + "px";
    this._spacer.style.height = newContainerSize.height + "px";
  }

  public setContainerScrollPosition(position: Position2D) {
    this._container.scrollLeft = position.x;
    this._container.scrollTop = position.y;
  }

  public setThumnailSelected(id: string, selected: boolean) {
    const thumb = this._container.querySelector("[data-id='" + CSS.escape(id) + "']");

    if(!thumb){
        return;
    }

    if (selected) {
      thumb.classList.add("selected");
    } else {
      thumb.classList.remove("selected");
    }
  }

  public injectTile(id: string, position: Position2D, label: string) {
    const thumbnailImage = document.createElement("img");
    thumbnailImage.setAttribute("alt", label);

    const thumbnailLabel = document.createElement("div");
    thumbnailLabel.innerText = label;
    thumbnailLabel.classList.add("caption");

    const imageSpacer = document.createElement("div");
    imageSpacer.classList.add("imgSpacer");
    imageSpacer.append(thumbnailImage);

    const thumbnailDiv = document.createElement("div");
    thumbnailDiv.setAttribute("data-id", id);
    thumbnailDiv.classList.add("iviewThumbnail");
    thumbnailDiv.classList.add("thumbnail");
    thumbnailDiv.prepend(imageSpacer);
    thumbnailDiv.append(thumbnailLabel);
    thumbnailDiv.style.left = (this.gap + position.x) + "px";
    thumbnailDiv.style.top = position.y + "px";

    this._inputHandler.addedThumbnail(id, thumbnailDiv);

    this._container.append(thumbnailDiv);
  }

  public updateTileHref(id: string, href: string) {
    let htmlImageElement = this._container.querySelector("div[data-id='" + CSS.escape(id) + "'] img") as (HTMLImageElement|null);
    if(htmlImageElement != null){
      htmlImageElement.src =  href;
    }
  }

  public removeTile(id: string) {
    let thumbnailDivElement = this._container.querySelector("div[data-id='" + CSS.escape(id) + "']");
    if(thumbnailDivElement != null){
      thumbnailDivElement.remove();
    }
  }

  public updateTilePosition(id: string, position: Position2D) {
    const thumbnailDiv = this._container.querySelector("div[data-id='" + id + "']") as HTMLDivElement;
    if(thumbnailDiv){
      thumbnailDiv.style.left = this.gap + position.x + "px";
      thumbnailDiv.style.top = position.y + "px";
    }
  }

  public getViewportSize(): Size2D {
    return new Size2D(getElementWidth(this._container), getElementHeight(this._container));
  }


  public jumpToThumbnail(thumbnailPos: number) {
    this._container.scrollTop = thumbnailPos;
  }
}

export interface ThumbnailOverviewResizeHandler {
  resized(newViewPort: Size2D): void;
}

export interface ThumbnailOverviewScrollHandler {
  scrolled(newPosition: Position2D): void;
}

