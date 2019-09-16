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

/// <reference path="../../base/widgets/canvas/TileImagePage.ts" />

namespace mycore.viewer.widgets.canvas {

    export class TileImagePageIIIF extends canvas.TileImagePage {

        protected loadTile(tilePos: Position3D) {
            let iiifPos = this.tilePosToIIIFPos(tilePos);
            if (this._tiles.has(tilePos)) {
                return this._tiles.get(tilePos);
            } else {
                if (!this._loadingTiles.has(tilePos)) {
                    this._loadTileIIIF(tilePos, iiifPos, (img: HTMLImageElement) => {
                        this._tiles.set(tilePos, img);
                        if (typeof this.refreshCallback != "undefined" && this.refreshCallback != null) {
                            this._imgPreviewLoaded = true;
                            this._imgNotPreviewLoaded = true;
                            this.refreshCallback();
                        }
                    }, () => {
                            console.error("Could not load tile : " + tilePos.toString());
                        });
                }

            }

            return null;
        }

        private tilePosToIIIFPos(tilePos: Position3D) {
            let iiifPos:any;
            iiifPos = tilePos;
            iiifPos.x = iiifPos.x * 256 * Math.pow(2, this.maxZoomLevel() - iiifPos.z);
            iiifPos.w = 256 * Math.pow(2, this.maxZoomLevel() - iiifPos.z);
            iiifPos.y = iiifPos.y * 256 * Math.pow(2, this.maxZoomLevel() - iiifPos.z);
            iiifPos.h = 256 * Math.pow(2, this.maxZoomLevel() - iiifPos.z);
            iiifPos.tx = ((iiifPos.x + iiifPos.w) > this._width) ? Math.ceil((this._width - iiifPos.x) / Math.pow(2, this.maxZoomLevel() - iiifPos.z)) : 256;
            iiifPos.ty = ((iiifPos.y + iiifPos.h) > this._height) ? Math.ceil((this._height - iiifPos.y) / Math.pow(2, this.maxZoomLevel() - iiifPos.z)) : 256;
            return iiifPos;
        }

        private _loadTileIIIF(tilePos: Position3D, iiifPos: any, okCallback: (image: HTMLImageElement) => void, errorCallback: () => void): void {
            var pathSelect = Utils.hash(tilePos.toString()) % this._tilePath.length;

            var path = this._tilePath[pathSelect];
            var image = new Image();


            image.onload = () => {
                this._loadingTiles.remove(tilePos);
                okCallback(image);
            };

            image.onerror = () => {
                errorCallback();
            };
            image.src = ViewerFormatString(path, iiifPos);
            this._loadingTiles.set(tilePos, image);
        }
    }
}
