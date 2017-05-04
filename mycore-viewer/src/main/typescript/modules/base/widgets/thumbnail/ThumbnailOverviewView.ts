namespace mycore.viewer.widgets.thumbnail {
    export class ThumbnailOverviewView {
        constructor(private _container:JQuery, private _scrollHandler:ThumbnailOverviewScrollHandler, private _resizeHandler:ThumbnailOverviewResizeHandler, private _inputHandler:ThumbnailOverviewInputHandler) {
            this._gap = 0;
            this._spacer = jQuery("<div></div>");
            this._spacer.appendTo(this._container);


            var cssObj = { "position": "relative" };

            cssObj["overflow-y"] = "scroll";
            cssObj["overflow-x"] = "hidden";
            cssObj["-webkit-overflow-scrolling"] = "touch";

            this._container.css(cssObj);
            this._lastViewPortSize = this.getViewportSize();
            var that = this;
            var scrollHandler = function () {
                var newPos = new Position2D(that._container.scrollLeft(), that._container.scrollTop());
                that._scrollHandler.scrolled(newPos);
            };

            // TODO: Use touch
            this._container.bind("scroll", scrollHandler);


            var resizeHandler = function () {
                var newVp = that.getViewportSize();
                if (that._lastViewPortSize != newVp) {
                    that._resizeHandler.resized(newVp);
                    that._lastViewPortSize = that.getViewportSize();
                    scrollHandler();
                }
            }

            jQuery(this._container).bind("iviewResize", resizeHandler);

        }

        private _gap:number;
        private _lastViewPortSize:Size2D;
        private _spacer:JQuery;

        public set gap(num:number) {
            this._gap = num;
        }

        public get gap() {
            return this._gap;
        }

        public setContainerSize(newContainerSize:Size2D) {
            this._spacer.css({
                "width": newContainerSize.width,
                "height": newContainerSize.height
            });
        }

        public setContainerScrollPosition(position:Position2D) {
            this._container.scrollLeft(position.x);
            this._container.scrollTop(position.y);
        }

        public setThumnailSelected(id:string, selected:boolean) {
            var thumb = this._container.find("[data-id='" + id + "']");

            if (selected) {
                thumb.addClass("selected");
            } else {
                thumb.removeClass("selected");
            }
        }

        public injectTile(id:string, position:Position2D, label:string) {
            var thumbnailImage = jQuery("<img />");
            thumbnailImage.attr("alt", label);

            var thumbnailLabel = jQuery("<div>" + label + "</div>");
            thumbnailLabel.addClass("caption");

            var imageSpacer = jQuery("<div></div>");
            imageSpacer.addClass("imgSpacer");
            imageSpacer.append(thumbnailImage);
            
            var thumbnailDiv = jQuery("<div/>");
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

        public updateTileHref(id:string, href:string) {
            this._container.find("div[data-id=" + id + "] img").attr("src", href);
        }

        public removeTile(id:string) {
            this._container.find("div[data-id=" + id + "]").remove();

        }

        public updateTilePosition(id:string, position:Position2D) {
            var thumbnailDiv = this._container.find("div[data-id='" + id + "']");
            thumbnailDiv.css({
                /* "display": "block" ,*/
                /*"position": "relative",*/
                "left": this.gap + position.x,
                "top": position.y
            });
        }

        public getViewportSize():Size2D {
            return new Size2D(this._container.width(), this._container.height());
        }


        public jumpToThumbnail(thumbnailPos:number) {
            this._container.scrollTop(thumbnailPos);
        }
    }
    export interface ThumbnailOverviewResizeHandler {
        resized(newViewPort:Size2D): void;
    }

    export interface ThumbnailOverviewScrollHandler {
        scrolled(newPosition:Position2D): void;
    }
}