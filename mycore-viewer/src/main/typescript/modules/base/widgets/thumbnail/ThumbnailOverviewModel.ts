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