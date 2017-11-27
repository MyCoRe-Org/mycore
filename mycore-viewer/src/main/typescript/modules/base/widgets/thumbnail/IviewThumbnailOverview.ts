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

/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="../../Utils.ts" />
/// <reference path="ThumbnailOverviewThumbnail.ts" />
/// <reference path="ThumbnailOverviewModel.ts" />
/// <reference path="ThumbnailOverviewView.ts" />
/// <reference path="ThumbnailOverviewSettings.ts" />

namespace mycore.viewer.widgets.thumbnail {
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
            var vpSize = this._view.getViewportSize();
            var maxTileSize = this._settings.maxThumbnailSize;
            var scrollPos = this._model.currentPosition;
            var tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
            var thumb = this._model.getThumbnailById(id);
            var pos = (<Array<ThumbnailOverviewThumbnail>>this._model.thumbnails).indexOf(thumb);
            var verticalLinePos = Math.floor(pos / tilesHorizontal);
            var verticalPos = verticalLinePos * this._settings.maxThumbnailSize.height;

            var isOver = this._model.currentPosition.y > verticalPos;
            var isUnder = this._model.currentPosition.y + this._view.getViewportSize().height < verticalPos + this._settings.maxThumbnailSize.height;
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
            var vpSize = this._view.getViewportSize();

            var sizeOfOther = ((childs: JQuery) => {
                var height = 0;
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

            var gap = (vpSize.width % this._settings.maxThumbnailSize.width) / 2;
            this._view.gap = gap;

            var maxTileSize = this._settings.maxThumbnailSize;
            var pos = this._model.currentPosition;

            // Container Size
            var tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
            var tilesVertical = Math.ceil(vpSize.height / maxTileSize.height);

            if (resize) {
                this._view.setContainerSize(new Size2D(vpSize.width,
                    Math.ceil(this._model.thumbnails.length / Math.max(tilesHorizontal, 1)) * maxTileSize.height));
                this.updateThumbnails(0, Math.ceil(this._model.thumbnails.length / tilesHorizontal), resize);
            }

            var startLine = Math.floor(pos.y / maxTileSize.height);
            var endLine = Math.ceil((pos.y / maxTileSize.height) + tilesVertical);
            this.updateThumbnails(startLine, endLine, false);
        }

        private updateThumbnails(startLine: number, endLine: number, positionOnly: boolean) {
            var vpSize = this._view.getViewportSize();
            var maxTileSize = this._settings.maxThumbnailSize;
            var tilesHorizontal = Math.floor(vpSize.width / maxTileSize.width);
            var dontRemoveMap = new MyCoReMap<string, boolean>();
            var that = this;

            for (var tileY = startLine || 0; tileY < endLine; tileY++) {
                for (var tileX = 0; tileX < tilesHorizontal; tileX++) {
                    var tileNumber = (tileY * tilesHorizontal) + tileX;
                    var tilePosition = new Position2D(tileX * maxTileSize.width, tileY * maxTileSize.height);

                    var tile = <ThumbnailOverviewThumbnail>this._model.thumbnails[tileNumber];
                    var tileExists = typeof tile != "undefined";

                    if (tileExists) {
                        var tileInserted = this._model.tilesInsertedMap.get(tile.id);
                        if (!tileInserted && !positionOnly) {
                            this._model.tilesInsertedMap.set(tile.id, true)
                            this._view.injectTile(tile.id, tilePosition, tile.label);
                            tile.requestImgdataUrl(((id) => (href: string) => {
                                that._view.updateTileHref(id, href);
                            })(tile.id));
                            if (this._model.selectedThumbnail != null && this._model.selectedThumbnail.href == tile.href) {
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


            var removeableBefore = (startLine - 1 * tilesHorizontal);
            var removeableAfter = (endLine + 1 * tilesHorizontal);

            var that = this;
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
