/// <reference path="../../definitions/jquery.d.ts" />
/// <reference path="../../Utils.ts" />

/// <reference path="ThumbnailOverviewThumbnail.ts" />
/// <reference path="ThumbnailOverviewInputHandler.ts" />

namespace mycore.viewer.widgets.thumbnail {

    export interface ThumbnailOverviewSettings {
        /**
         * Array of all Thumbnails
         */
        thumbnails: Array<ThumbnailOverviewThumbnail>;

        /**
         * Container needs a size(width/height).
         */
        container: JQuery;

        /**
         * Maximal Size a Thumbnail should displayed. (this should include the gaps between thumbnails)
         */
        maxThumbnailSize: Size2D;

        inputHandler: ThumbnailOverviewInputHandler;
    }

    export class DefaultThumbnailOverviewSettings implements ThumbnailOverviewSettings {
        constructor(private _thumbnails: Array<ThumbnailOverviewThumbnail>, private _container: JQuery, private _inputHandler: ThumbnailOverviewInputHandler = { addedThumbnail: function(id:string, element:JQuery) { } }, private _maxThumbnailSize = new Size2D(255, 255)) {
        }

        public get thumbnails() {
            return this._thumbnails;
        }

        public get container() {
            return this._container;
        }

        public get maxThumbnailSize() {
            return this._maxThumbnailSize;
        }

        public get inputHandler(): ThumbnailOverviewInputHandler {
            return this._inputHandler;
        }


    }

}