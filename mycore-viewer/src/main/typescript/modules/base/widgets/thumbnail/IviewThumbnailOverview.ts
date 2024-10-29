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


import { MoveVector, MyCoReMap, Position2D, Size2D } from "../../Utils";
import {
  ThumbnailOverviewResizeHandler,
  ThumbnailOverviewScrollHandler,
  ThumbnailOverviewView
} from "./ThumbnailOverviewView";
import { ThumbnailOverviewSettings } from "./ThumbnailOverviewSettings";
import { ThumbnailOverviewModel } from "./ThumbnailOverviewModel";
import { ThumbnailOverviewThumbnail } from "./ThumbnailOverviewThumbnail";

export class IviewThumbnailOverview implements ThumbnailOverviewScrollHandler, ThumbnailOverviewResizeHandler {

  constructor(private _settings: ThumbnailOverviewSettings) {
    this._model = new ThumbnailOverviewModel(this._settings.thumbnails);
    this._view = new ThumbnailOverviewView(this._settings.container, this, this, this._settings.inputHandler);
    this._settings.container.css({
      "min-width": this._settings.maxThumbnailSize.width + "px",
      "min-height": this._settings.maxThumbnailSize.height + "px"
    });
    this.update(true);

  }

  private _view: ThumbnailOverviewView;
  private _model: ThumbnailOverviewModel;

  public setThumbnailSelected(id: string) {
    if (typeof this._model.selectedThumbnail !== "undefined" && this._model.selectedThumbnail != null) {
      this._view.setThumnailSelected(this._model.selectedThumbnail.id, false);
    }
    this._model.selectedThumbnail = this._model.getThumbnailById(id) || null;
    this._view.setThumnailSelected(id, true);
  }

  public jumpToThumbnail(id: string) {
    const vpSize = this._view.getViewportSize();
    const maxTileSize = this._settings.maxThumbnailSize;
    const scrollPos = this._model.currentPosition;
    const tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
    const thumb = this._model.getThumbnailById(id);
    const pos = (<Array<ThumbnailOverviewThumbnail>>this._model.thumbnails).indexOf(thumb);
    const verticalLinePos = Math.floor(pos / tilesHorizontal);
    let verticalPos = verticalLinePos * this._settings.maxThumbnailSize.height;

    const isOver = this._model.currentPosition.y > verticalPos;
    const isUnder = this._model.currentPosition.y + this._view.getViewportSize().height < verticalPos + this._settings.maxThumbnailSize.height;
    if (isOver) {
    } else if (isUnder) {
      verticalPos = verticalPos - this._view.getViewportSize().height + this._settings.maxThumbnailSize.height;
    } else {
      return;
    }

    this._model.currentPosition.move(new MoveVector(0, verticalPos - scrollPos.y));
    this.update();
    this._view.jumpToThumbnail(verticalPos);

  }


  public update(resize: boolean = false): void {
    const vpSize = this._view.getViewportSize();

    const sizeOfOther = ((childs: JQuery) => {
      let height = 0;
      childs.each((i, e: Element) => {
        if (this._settings.container[0] != e && jQuery(e).css("position") != "absolute") {
          height += jQuery(e).outerHeight();
        }
      });
      return height;
    })(this._settings.container.parent().children());


    this._settings.container.css({ "height": this._settings.container.parent().height() - sizeOfOther });
    if (vpSize.width == 0 || vpSize.height == 0) {
      return;
    }

    const gap = (vpSize.width % this._settings.maxThumbnailSize.width) / 2;
    this._view.gap = gap;

    const maxTileSize = this._settings.maxThumbnailSize;
    const pos = this._model.currentPosition;

    // Container Size
    const tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
    const tilesVertical = Math.ceil(vpSize.height / maxTileSize.height);

    if (resize) {
      this._view.setContainerSize(new Size2D(vpSize.width,
        Math.ceil(this._model.thumbnails.length / Math.max(tilesHorizontal, 1)) * maxTileSize.height));
      this.updateThumbnails(0, Math.ceil(this._model.thumbnails.length / tilesHorizontal), resize);
    }

    const startLine = Math.floor(pos.y / maxTileSize.height);
    const endLine = Math.ceil((pos.y / maxTileSize.height) + tilesVertical);
    this.updateThumbnails(startLine, endLine, false);
  }

  private updateThumbnails(startLine: number, endLine: number, positionOnly: boolean) {
    const vpSize = this._view.getViewportSize();
    const maxTileSize = this._settings.maxThumbnailSize;
    const tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
    const dontRemoveMap = new MyCoReMap<string, boolean>();
    const that = this;

    for (let tileY = startLine || 0; tileY < endLine; tileY++) {
      for (let tileX = 0; tileX < tilesHorizontal; tileX++) {
        const tileNumber = (tileY * tilesHorizontal) + tileX;
        const tilePosition = new Position2D(tileX * maxTileSize.width, tileY * maxTileSize.height);

        const tile = this._model.thumbnails[tileNumber];
        const tileExists = typeof tile != "undefined";

        if (tileExists) {
          const tileInserted = this._model.tilesInsertedMap.get(tile.id);
          if (!tileInserted && !positionOnly) {
            this._model.tilesInsertedMap.set(tile.id, true)
            this._view.injectTile(tile.id, tilePosition, tile.label);
            tile.requestImgdataUrl(((id) => (href: string) => {
              that._view.updateTileHref(id, href);
            })(tile.id));
            if (this._model.selectedThumbnail != null && this._model.selectedThumbnail.id === tile.id) {
              this.setThumbnailSelected(tile.id);
            }
          } else {
            if (tileInserted) {
              this._view.updateTilePosition(tile.id, tilePosition);
            }
          }
          dontRemoveMap.set(tile.id, false);
        }

      }
    }

    this._model.tilesInsertedMap.forEach(function(k, v) {
      if (!dontRemoveMap.has(k) && dontRemoveMap.get(k) != false) {
        that.removeThumbnail(k);
      }
    });


  }

  private removeThumbnail(tileId: string) {
    this._model.tilesInsertedMap.remove(tileId);
    this._view.removeTile(tileId);

  }

  scrolled(newPosition: Position2D): void {
    this._model.currentPosition = newPosition;
    this.update(false);
  }

  resized(newViewPort: Size2D): void {
    this.update(true);
    if (this._model.selectedThumbnail != null) {
      this.jumpToThumbnail(this._model.selectedThumbnail.id);
    }
  }

}


/**
 *             for (var tileY = 0; tileY < tilesVertical; tileY++) {
 for (var tileX = startLine; tileX < endLine; tileX++) {
 var tileNumber = (tileX * tilesVertical) + tileY;
 var tilePosition = new Position2D(tileX * maxTileSize.width, tileY * maxTileSize.height);

 var tile = this._model.thumbnails[tileNumber];

 if (!this._model.tilesInsertedMap.get(tile.id)) {
 this._view.injectTile(tile.id, tilePosition, tile.href, tile.label);
 }
 }
 }
 */
