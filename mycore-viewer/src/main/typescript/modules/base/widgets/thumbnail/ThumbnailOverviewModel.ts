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

/// <reference path="ThumbnailOverviewThumbnail.ts" />
/// <reference path="../../Utils.ts" />

namespace mycore.viewer.widgets.thumbnail {
    export class ThumbnailOverviewModel {
        constructor(public thumbnails:Array<ThumbnailOverviewThumbnail> = new Array<ThumbnailOverviewThumbnail>()) {
            this._idThumbnailMap = new MyCoReMap<string, ThumbnailOverviewThumbnail>();
            this.tilesInsertedMap = new MyCoReMap<string, boolean>();
            this.fillIdThumbnailMap();
            this.fillTilesInsertedMap();
            this.currentPosition = new Position2D(0, 0);
        }

        private _idThumbnailMap:MyCoReMap<string, ThumbnailOverviewThumbnail>;

        public selectedThumbnail:ThumbnailOverviewThumbnail;
        public currentPosition:Position2D;
        public tilesInsertedMap:MyCoReMap<string, boolean>;


        private fillTilesInsertedMap():void {
            for (var index in this.thumbnails) {
                var current:ThumbnailOverviewThumbnail = <any>this.thumbnails[index];

                this.tilesInsertedMap.set(current.id, false);
            }
        }

        private fillIdThumbnailMap():void {
            for (var index in this.thumbnails) {
                var current:ThumbnailOverviewThumbnail = <any>this.thumbnails[index];
                this._idThumbnailMap.set(current.id, current);
            }
        }

        public getThumbnailById(id:string) {
            return this._idThumbnailMap.get(id);
        }


    }
}
