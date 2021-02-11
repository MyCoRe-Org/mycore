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

namespace mycore.viewer.widgets.canvas {

    export class TileImagePage implements model.AbstractPage {

        constructor(public id: string, protected width: number, protected height: number, tilePaths: string[]) {
            this.vTilePath = tilePaths;
            this.loadTile(new Position3D(0, 0, 0));

        }

        protected static TILE_SIZE: number = 256;
        protected vTilePath: string[];
        protected vTiles: MyCoReMap<Position3D, HTMLImageElement> = new MyCoReMap<Position3D, HTMLImageElement>();
        protected vLoadingTiles: MyCoReMap<Position3D, HTMLImageElement> = new MyCoReMap<Position3D, HTMLImageElement>();

        protected vBackBuffer: HTMLCanvasElement = document.createElement('canvas');
        protected vBackBufferArea: Rect = null;
        protected vBackBufferAreaZoom: number = null;

        protected vPreviewBackBuffer: HTMLCanvasElement = document.createElement('canvas');
        protected vPreviewBackBufferArea: Rect = null;
        protected vPreviewBackBufferAreaZoom: number = null;

        protected vImgPreviewLoaded: boolean = false;
        protected vImgNotPreviewLoaded: boolean = false;

        protected htmlContent: ViewerProperty<HTMLElement> = new ViewerProperty<HTMLElement>(this, 'htmlContent');

        public get size(): Size2D {
            return new Size2D(this.width, this.height);
        }

        public refreshCallback: () => void;

        public draw(ctx: CanvasRenderingContext2D, rect: Rect, scale: number, overview: boolean): void {
            if (rect.pos.x < 0 || rect.pos.y < 0) {
                rect = new Rect(rect.pos.max(0, 0), rect.size);
            }

            const zoomLevel = Math.min(this.getZoomLevel(scale), this.maxZoomLevel());
            const zoomLevelScale = this.scaleForLevel(zoomLevel);

            const diff = scale / zoomLevelScale;

            const tileSizeInZoomLevel = TileImagePage.TILE_SIZE / zoomLevelScale;

            const startX = Math.floor(rect.pos.x / tileSizeInZoomLevel);
            const startY = Math.floor(rect.pos.y / tileSizeInZoomLevel);
            const endX = Math.ceil(Math.min(rect.pos.x + rect.size.width, this.size.width) / tileSizeInZoomLevel);
            const endY = Math.ceil(Math.min(rect.pos.y + rect.size.height, this.size.height) / tileSizeInZoomLevel);

            this.updateBackbuffer(startX, startY, endX, endY, zoomLevel, overview);

            ctx.save();
            {
                const xBase = (startX * tileSizeInZoomLevel - rect.pos.x) * scale;
                const yBase = (startY * tileSizeInZoomLevel - rect.pos.y) * scale;
                ctx.translate(xBase, yBase);
                ctx.scale(diff, diff);
                if (overview) {
                    ctx.drawImage(this.vPreviewBackBuffer, 0, 0);
                } else {
                    ctx.drawImage(this.vBackBuffer, 0, 0);
                }
            }
            ctx.restore();

        }

        public getHTMLContent() {
            return this.htmlContent;
        }

        protected updateHTML() {
            if (typeof this.refreshCallback !== 'undefined' && this.refreshCallback !== null) {
                this.refreshCallback();
            }
        }

        public clear() {
            this.abortLoadingTiles();
            this.vBackBuffer.width = 1;
            this.vBackBuffer.height = 1;
            this.vBackBufferAreaZoom = null;
            let tile: HTMLImageElement = null;

            const previewTilePos = new Position3D(0, 0, 0);
            const hasPreview = this.vTiles.has(previewTilePos);

            if (hasPreview) {
                tile = this.vTiles.get(previewTilePos);
            }

            this.vTiles.clear();

            if (hasPreview) {
                this.vTiles.set(previewTilePos, tile);
            }

            this.vLoadingTiles.clear();
        }

        protected updateBackbuffer(startX: number, startY: number,
                                   endX: number, endY: number, zoomLevel: number, overview: boolean) {
            const newBackBuffer = new Rect(new Position2D(startX, startY), new Size2D(endX - startX, endY - startY));
            if (overview) {
                if (this.vPreviewBackBufferArea !== null
                    && !this.vImgPreviewLoaded
                    && this.vPreviewBackBufferArea.equals(newBackBuffer)
                    && zoomLevel === this.vPreviewBackBufferAreaZoom) {
                    return;
                } else {
                    this.vPreviewBackBuffer.width = newBackBuffer.size.width * 256;
                    this.vPreviewBackBuffer.height = newBackBuffer.size.height * 256;
                    this.drawToBackbuffer(startX, startY, endX, endY, zoomLevel, true);
                }
                this.vPreviewBackBufferArea = newBackBuffer;
                this.vPreviewBackBufferAreaZoom = zoomLevel;
                this.vImgPreviewLoaded = false;
            } else {
                if (this.vBackBufferArea !== null
                    && !this.vImgNotPreviewLoaded
                    && this.vBackBufferArea.equals(newBackBuffer)
                    && zoomLevel === this.vBackBufferAreaZoom) {
                    // backbuffer content is the same
                    return;
                } else {
                    // need to draw the full buffer, because zoom level changed or never drawed before
                    this.vBackBuffer.width = newBackBuffer.size.width * 256;
                    this.vBackBuffer.height = newBackBuffer.size.height * 256;
                    this.drawToBackbuffer(startX, startY, endX, endY, zoomLevel, false);
                }
                this.vBackBufferArea = newBackBuffer;
                this.vBackBufferAreaZoom = zoomLevel;
                this.vImgNotPreviewLoaded = false;
            }

            /*
             else {
             // zoom level is the same, so look for copy old contents
             var reusableContent = this._backBufferArea.getIntersection(newBackBuffer);
             if (reusableContent == null) {
             // content complete changed :(
             this._drawToBackbuffer(startX, startY, endX, endY, zoomLevel);
             } else {
             // we can copy old content \o/
             // calculate were the old content is in the new backbuffer (in px)
             var xTranslate = reusableContent.pos.x - newBackBuffer.pos.x * 256;
             var yTranslate = reusableContent.pos.y - newBackBuffer.pos.y * 256;

             var ctx = this._backBuffer.getContext("2d");
             ctx.save();
             ctx.translate(xTranslate, yTranslate);
             this._drawToBackbuffer(reusableContent.pos.x, reusableContent.pos.y,
                reusableContent.pos.x + reusableContent.size.width,
                reusableContent.pos.y + reusableContent.size.height, zoomLevel);
             ctx.restore();
             }
             }         */

        }

        protected static EMPTY_FUNCTION = () => {
        };

        protected abortLoadingTiles() {
            this.vLoadingTiles.forEach((k, v) => {
                v.onerror = TileImagePage.EMPTY_FUNCTION;
                v.onload = TileImagePage.EMPTY_FUNCTION;
                v.src = '#';
            });
            this.vLoadingTiles.clear();
        }

        protected drawToBackbuffer(startX: number, startY: number,
                                   endX: number, endY: number, zoomLevel: number, overview: boolean) {
            let ctx: CanvasRenderingContext2D;
            if (overview) {
                ctx = <CanvasRenderingContext2D> this.vPreviewBackBuffer.getContext('2d');
            } else {
                ctx = <CanvasRenderingContext2D> this.vBackBuffer.getContext('2d');
            }

            for (let x = startX; x < endX; x++) {
                for (let y = startY; y < endY; y++) {
                    const tilePosition = new Position3D(x, y, zoomLevel);
                    const tile = this.loadTile(tilePosition);
                    const rasterPositionX = (x - startX) * 256;
                    const rasterPositionY = (y - startY) * 256;

                    if (tile !== null) {
                        ctx.drawImage(tile, Math.floor(rasterPositionX),
                            rasterPositionY, tile.naturalWidth, tile.naturalHeight);
                    } else {
                        const preview = this.getPreview(tilePosition);
                        if (preview !== null) {
                            this.drawPreview(ctx, new Position2D(rasterPositionX, rasterPositionY), preview);
                        }
                    }
                }
            }
        }

        protected drawPreview(ctx: CanvasRenderingContext2D, targetPosition: Position2D, tile: PreviewTile) {
            tile.areaToDraw.size.width = Math.min(tile.areaToDraw.pos.x + tile.areaToDraw.size.width,
                tile.tile.naturalWidth) - tile.areaToDraw.pos.x;
            tile.areaToDraw.size.height = Math.min(tile.areaToDraw.pos.y + tile.areaToDraw.size.height,
                tile.tile.naturalHeight) - tile.areaToDraw.pos.y;

            ctx.drawImage(tile.tile,
                tile.areaToDraw.pos.x,
                tile.areaToDraw.pos.y,
                tile.areaToDraw.size.width,
                tile.areaToDraw.size.height,
                targetPosition.x,
                targetPosition.y,
                tile.areaToDraw.size.width * tile.scale,
                tile.areaToDraw.size.height * tile.scale
                );
        }

        protected loadTile(tilePos: Position3D) {
            if (this.vTiles.has(tilePos)) {
                return this.vTiles.get(tilePos);
            } else {
                if (!this.vLoadingTiles.has(tilePos)) {
                    this._loadTile(tilePos, (img: HTMLImageElement) => {
                        this.vTiles.set(tilePos, img);
                        if (typeof this.refreshCallback !== 'undefined' && this.refreshCallback !== null) {
                            this.vImgPreviewLoaded = true;
                            this.vImgNotPreviewLoaded = true;
                            this.refreshCallback();
                        }
                    }, () => {
                            console.error('Could not load tile : ' + tilePos.toString());
                        });
                }

            }

            return null;
        }

        /**
         * Gets a preview draw instruction for a specific tile.
         * param tilePos the tile
         * returns { tile:HTMLImageElement; areaToDraw: Rect } tile contains the Image to draw and areaToDraw
         * contains the coordinates in the Image.
         */
        protected getPreview(tilePos: Position3D, scale: number = 1): PreviewTile {
            if (this.vTiles.has(tilePos)) {
                const tile = this.vTiles.get(tilePos);
                return { tile: tile, areaToDraw: new Rect(new Position2D(0, 0), new Size2D(256, 256)), scale: scale };
            } else {
                const newZoom = tilePos.z - 1;

                if (newZoom < 0) {
                    return null;
                }

                const newPos = new Position2D(Math.floor(tilePos.x / 2), Math.floor(tilePos.y / 2));
                const xGridPos = tilePos.x % 2;
                const yGridPos = tilePos.y % 2;

                const prev = this.getPreview(new Position3D(newPos.x, newPos.y, newZoom), scale * 2);
                if (prev !== null) {
                    const newAreaSize = new Size2D(prev.areaToDraw.size.width / 2, prev.areaToDraw.size.height / 2);
                    const newAreaPos = new Position2D(
                        prev.areaToDraw.pos.x + (newAreaSize.width * xGridPos),
                        prev.areaToDraw.pos.y + (newAreaSize.height * yGridPos)
                    );

                    return {
                        tile: prev.tile,
                        areaToDraw: new Rect(newAreaPos, newAreaSize),
                        scale: prev.scale
                    };
                } else {
                    return null;
                }
            }
        }

        protected maxZoomLevel(): number {
            return Math.max(Math.ceil(Math.log(Math.max(this.width, this.height) / TileImagePage.TILE_SIZE) / Math.LN2), 0);
        }

        protected getZoomLevel(scale: number): number {
            return Math.max(0, Math.ceil(this.maxZoomLevel() - Math.log(scale) / Utils.LOG_HALF));
        }

        protected scaleForLevel(level: number): number {
            return Math.pow(0.5, this.maxZoomLevel() - level);
        }

        protected _loadTile(tilePos: Position3D, okCallback: (image: HTMLImageElement) => void, errorCallback: () => void): void {
            const pathSelect = Utils.hash(tilePos.toString()) % this.vTilePath.length;

            const path = this.vTilePath[pathSelect];
            const image = new Image();


            image.onload = () => {
                this.vLoadingTiles.remove(tilePos);
                okCallback(image);
            };

            image.onerror = () => {
                errorCallback();
            };

            image.src = ViewerFormatString(path, tilePos);
            this.vLoadingTiles.set(tilePos, image);
        }

        toString(): string {
            return this.vTilePath[0];
        }
    }

    interface PreviewTile {
        tile: HTMLImageElement;
        areaToDraw: Rect;
        scale: number;
    }

}
