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
import { Position2D, Size2D } from "../../Utils";

export class ThumbnailOverviewView {
  constructor(private _container: JQuery,
    private _scrollHandler: ThumbnailOverviewScrollHandler,
    private _resizeHandler: ThumbnailOverviewResizeHandler,
    private _inputHandler: ThumbnailOverviewInputHandler) {
    this._gap = 0;
    this._spacer = jQuery("<div></div>");
    this._spacer.appendTo(this._container);


    const cssObj = { "position": "relative" };

    cssObj["overflow-y"] = "scroll";
    cssObj["overflow-x"] = "hidden";
    cssObj["-webkit-overflow-scrolling"] = "touch";

    this._container.css(cssObj);
    this._lastViewPortSize = this.getViewportSize();
    const scrollHandler = () => {
      const newPos = new Position2D(this._container.scrollLeft(), this._container.scrollTop());
      this._scrollHandler.scrolled(newPos);
    };

    // TODO: Use touch
    this._container.bind("scroll", scrollHandler);


    const resizeHandler = () => {
      const newVp = this.getViewportSize();
      if (this._lastViewPortSize != newVp) {
        this._resizeHandler.resized(newVp);
        this._lastViewPortSize = this.getViewportSize();
        scrollHandler();
      }
    }

    jQuery(this._container).bind("iviewResize", resizeHandler);

  }

  private _gap: number;
  private _lastViewPortSize: Size2D;
  private _spacer: JQuery;

  public set gap(num: number) {
    this._gap = num;
  }

  public get gap() {
    return this._gap;
  }

  public setContainerSize(newContainerSize: Size2D) {
    this._spacer.css({
      "width": newContainerSize.width,
      "height": newContainerSize.height
    });
  }

  public setContainerScrollPosition(position: Position2D) {
    this._container.scrollLeft(position.x);
    this._container.scrollTop(position.y);
  }

  public setThumnailSelected(id: string, selected: boolean) {
    const thumb = this._container.find("[data-id='" + CSS.escape(id) + "']");

    if (selected) {
      thumb.addClass("selected");
    } else {
      thumb.removeClass("selected");
    }
  }

  public injectTile(id: string, position: Position2D, label: string) {
    const thumbnailImage = jQuery("<img />");
    thumbnailImage.attr("alt", label);

    const thumbnailLabel = jQuery("<div>" + label + "</div>");
    thumbnailLabel.addClass("caption");

    const imageSpacer = jQuery("<div></div>");
    imageSpacer.addClass("imgSpacer");
    imageSpacer.append(thumbnailImage);

    const thumbnailDiv = jQuery("<div/>");
    thumbnailDiv.attr("data-id", id);
    thumbnailDiv.toggleClass("iviewThumbnail");
    thumbnailDiv.addClass("thumbnail");
    thumbnailDiv.prepend(imageSpacer);
    thumbnailDiv.append(thumbnailLabel);
    thumbnailDiv.css({
      /* "display": "block" ,*/
      /*"position": "relative",*/
      "left": this.gap + position.x,
      "top": position.y
    });

    this._inputHandler.addedThumbnail(id, thumbnailDiv);

    this._container.append(thumbnailDiv);
  }

  public updateTileHref(id: string, href: string) {
    this._container.find("div[data-id='" + CSS.escape(id) + "'] img").attr("src", href);
  }

  public removeTile(id: string) {
    this._container.find("div[data-id='" + CSS.escape(id) + "']").remove();
  }

  public updateTilePosition(id: string, position: Position2D) {
    const thumbnailDiv = this._container.find("div[data-id='" + id + "']");
    thumbnailDiv.css({
      /* "display": "block" ,*/
      /*"position": "relative",*/
      "left": this.gap + position.x,
      "top": position.y
    });
  }

  public getViewportSize(): Size2D {
    return new Size2D(this._container.width(), this._container.height());
  }


  public jumpToThumbnail(thumbnailPos: number) {
    this._container.scrollTop(thumbnailPos);
  }
}

export interface ThumbnailOverviewResizeHandler {
  resized(newViewPort: Size2D): void;
}

export interface ThumbnailOverviewScrollHandler {
  scrolled(newPosition: Position2D): void;
}

