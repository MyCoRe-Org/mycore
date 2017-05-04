namespace mycore.viewer.widgets.canvas {
    export class DoublePageLayout extends PageLayout {

        public get relocated():boolean {
            return false;
        }

        private _rotation:number = 0;
        private _currentPage:number = 0;

        public syncronizePages():void {
            var vp = this._pageController.viewport;
            var pageSizeWithSpace = this.getPageHeightWithSpace();
            var rowCount = this._model.pageCount / 2;

            this.correctViewport();

            var widthMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 1 : 2;
            var widthDivisor = (widthMultiplicator == 1) ? 2 : 1;
            this._horizontalScrollbar.areaSize = this._pageDimension.width * widthMultiplicator;
            this._verticalScrollbar.areaSize = rowCount * this.getPageHeightWithSpace();

            this._horizontalScrollbar.viewSize = this._pageController.viewport.size.width / this._pageController.viewport.scale;
            this._verticalScrollbar.viewSize = this._pageController.viewport.size.height / this._pageController.viewport.scale;

            this._horizontalScrollbar.position = vp.position.x - (vp.size.width / vp.scale / 2) + (this._pageDimension.width / widthDivisor);
            this._verticalScrollbar.position = vp.position.y - (vp.size.height / vp.scale / 2);
            var pagesPerLine = 2;

            var vpSizeInArea = vp.size.height / vp.scale;
            var yStart = vp.position.y - (vpSizeInArea / 2);
            var yEnd = vp.position.y + (vpSizeInArea / 2);
            var yStartOrder = Math.floor(yStart / pageSizeWithSpace) * pagesPerLine;
            var yEndOrder = Math.ceil(yEnd / pageSizeWithSpace) * pagesPerLine + (this.relocated ? 1 : 0);

            var pagesToCheck = this._insertedPages.slice(0);

            for (var y = yStartOrder; y <= yEndOrder; y++) {
                if (this._model.children.has(y) && pagesToCheck.indexOf(y) == -1) {
                    pagesToCheck.push(y);
                } else if (0 < y && y <= this._model.pageCount) {
                    this._pageLoader(y);
                }
            }

            pagesToCheck.forEach((img) => {
                this.updatePage(img);
            });
        }

        public clear():void {
            var pages = this._pageController.getPages().slice(0);
            this._insertedPages.splice(0, this._insertedPages.length);
            pages.forEach((p) => {
                this._pageController.removePage(p)
            });
        }

        public fitToScreen():void {
            var vp = this._pageController.viewport;
            if (vp.size.width != 0 && vp.size.height != 0) {
                var dpRect = this.getDoublePageRect(this.getCurrentPage());
                vp.setRect(dpRect);
            }
        }

        public calculatePageAreaInformation(order:number) {
            var imgSize = this._model.children.get(order).size;
            var pai = new widgets.canvas.PageAreaInformation();
            var pr = this.getPageRect(order);
            var page = this._model.children.get(order);

            pai.scale = Math.min(this._originalPageDimension.width / imgSize.width, this._originalPageDimension.height / imgSize.height);

            var realPageDimension = page.size.scale(pai.scale);//.getRotated(this._rotation);
            pai.position = new Position2D(pr.pos.x + realPageDimension.width / 2, pr.pos.y + realPageDimension.height / 2);
            pai.rotation = this._rotation;

            return pai;
        }

        public checkShouldBeInserted(order:number) {
            var vpRect = this._pageController.viewport.asRectInArea();
            var imagePos = this.getImageMiddle(order);
            var imageRect = new Rect(new Position2D(imagePos.x - (this._pageDimension.width), imagePos.y - (this._pageDimension.height / 2)), new Size2D(this._pageDimension.width * 2, this._pageDimension.height));
            return vpRect.getIntersection(imageRect) != null || this.getCurrentOverview().getIntersection(imageRect) != null;
        }

        public fitToWidth(attop:boolean = false):void {
            var pr = this.getDoublePageRect(this.getCurrentPage());
            this._pageController.viewport.position = pr.getMiddlePoint();
            this._pageController.viewport.scale = this._pageController.viewport.size.width / pr.size.width
        }

        public getCurrentPage():number {
            var pagesPerLine = 2;
            var vp = this._pageController.viewport;
            var pageSizeWithSpace = this.getPageHeightWithSpace();
            var rowCount = this._model.pageCount / 2;
            var vpSizeInArea = vp.size.height / vp.scale;
            var yStart = vp.position.y - (vpSizeInArea / 2);
            var yEnd = vp.position.y + (vpSizeInArea / 2);
            var yStartOrder = Math.floor(yStart / pageSizeWithSpace) * pagesPerLine;
            var yEndOrder = Math.ceil(yEnd / pageSizeWithSpace) * pagesPerLine + (this.relocated ? 1 : 0);

            var maxPage = -1;
            var maxCount = -1;
            var maxIsMiddle = false;
            var vpRect = vp.asRectInArea();
            for (var y = yStartOrder; y <= yEndOrder; y++) {
                var curRect = this.getPageRect(y).getIntersection(vpRect);
                if (curRect != null && (!maxIsMiddle || y == this._currentPage)) {
                    var curCount = curRect.size.getSurface();
                    if (maxCount < curCount) {
                        maxPage = y;
                        maxCount = curCount;
                        if (curRect.intersects(vp.position)) {
                            maxIsMiddle = true;
                        }
                    }
                }

            }


            return Math.max(-1, maxPage);
        }

        public jumpToPage(order:number):void {
            var middle = this.getImageMiddle(order);
            //this._pageController.viewport.position = new Position2D(0, middle.y);
            this._currentPage = order;
            this._pageController.viewport.setRect(this.getPageRect(order))
        }

        private getPageHeightSpace() {
            return ((this._pageDimension.height / 100) * 10);
        }

        private getPageHeightWithSpace() {
            var rotationMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 2 : 1;
            return (this._pageDimension.height * rotationMultiplicator) + this.getPageHeightSpace();
        }

        public getImageMiddle(order:number) {
            var pageRect = this.getPageRect(order);
            return pageRect.getMiddlePoint();
        }

        private correctViewport():void {
            var vp = this._pageController.viewport;
            var widthMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 1 : 2;

            var pageScaling = this.getCurrentPageScaling();
            if (pageScaling != -1) {
                var minWidthScale = this._pageController.viewport.size.width / (this._pageDimension.width * widthMultiplicator);
                var minScale = Math.min(this._pageController.viewport.size.height / (this._pageDimension.height * 2), minWidthScale);

                if (vp.scale < minScale) {
                    vp.stopAnimation();
                    vp.scale = minScale;

                }

                var completeScale = vp.scale * pageScaling;
                if (completeScale > 4) {
                    vp.stopAnimation();
                    vp.scale = 4 / pageScaling;
                }
            }


            var scaledViewport = vp.size.scale(1 / vp.scale);
            var minY = 1;
            var maxY = this._model.pageCount / 2 * this.getPageHeightWithSpace();
            var correctedY = Math.min(Math.max(vp.position.y, minY), maxY);
            if (scaledViewport.width > (this._pageDimension.width * widthMultiplicator)) {
                var corrected = new Position2D(0, correctedY);
                if (!vp.position.equals(corrected)) {
                    vp.position = new Position2D(0, correctedY);
                }
            } else {
                var minimalX = -this._pageDimension.width + scaledViewport.width / 2;
                var maximalX = this._pageDimension.width - scaledViewport.width / 2;
                var correctedX = Math.max(minimalX, Math.min(maximalX, vp.position.x));
                var corrected = new Position2D(correctedX, correctedY);
                if (!vp.position.equals(corrected)) {
                    vp.position = corrected;
                }
            }
        }

        public scrollhandler() {
            if (this._pageController.viewport.currentAnimation == null) {
                var widthDivisor = (this._rotation == 90 || this._rotation == 270) ? 2 : 1;
                var vp = this._pageController.viewport;
                var scrollPos = new Position2D(this._horizontalScrollbar.position, this._verticalScrollbar.position);

                var xPos = scrollPos.x + (vp.size.width / vp.scale / 2) - (this._pageDimension.width / widthDivisor);

                vp.position = new Position2D(xPos, scrollPos.y + (vp.size.height / vp.scale / 2));
            }
        }

        public rotate(deg:number):void {
            var currentPage = this.getCurrentPage();
            this._pageDimension = this._originalPageDimension.getRotated(deg);
            this._rotation = deg;

            this.clear();
            this.syncronizePages();
            this.jumpToPage(currentPage);
        }

        public getLabelKey():string {
            return "doublePageLayout";
        }

        public getCurrentOverview():Rect {
            var doublePageRect = this.getDoublePageRect(this.getCurrentPage());
            return doublePageRect;
        }

        public getDoublePageRect(order:number):Rect {
            var row = Math.floor(((order - 1) + (this.relocated ? 1 : 0)) / 2);
            var firstPage = (order - 1 + (this.relocated ? 1 : 0)) % 2 == 0;

            var start;
            if (firstPage) {
                start = order;
            } else {
                start = order - 1;
            }

            var p1Rect = this.getPageRect(start);
            var p2Rect = this.getPageRect(start + 1);

            var bounding = Rect.getBounding(p1Rect, p2Rect);
            return bounding;
        }

        public getPageRect(order:number):Rect {
            var row = Math.floor(((order - 1) + (this.relocated ? 1 : 0)) / 2);
            var firstPage = (order - 1 + (this.relocated ? 1 : 0)) % 2 == 0;
            var pageDimension = this.getRealPageDimension(order);
            var yPos = (row * this.getPageHeightWithSpace());

            switch (this._rotation) {
                case 0:
                case 180:
                    if (this._rotation == 0 ? firstPage : !firstPage) {
                        return new Rect(new Position2D(-pageDimension.width, yPos), pageDimension);
                    } else {
                        return new Rect(new Position2D(0, yPos), pageDimension);
                    }
                case 90:
                case 270:
                    if (this._rotation == 90 ? firstPage : !firstPage) {
                        return new Rect(new Position2D(-pageDimension.width / 2, yPos - (pageDimension.height / 2)), pageDimension);
                    } else {
                        return new Rect(new Position2D(-pageDimension.width / 2, yPos + (this._pageDimension.height) - (pageDimension.height / 2)), pageDimension);
                    }
            }
        }

        public next():void {
            var page = this.getCurrentPage();
            var firstPage = (page - 1 + (this.relocated ? 1 : 0)) % 2 == 0 && (this._rotation == 0 || this._rotation == 180);
            var nextPage = Math.max(Math.min(page + (firstPage ? 2 : 1), this._model.pageCount), 0);
            this.jumpToPage(nextPage);
        }

        public previous():void {
            var page = this.getCurrentPage();
            var firstPage = (page - 1 + (this.relocated ? 1 : 0)) % 2 == 0 && (this._rotation == 0 || this._rotation == 180);
            var previousPage = Math.max(Math.min(page - (firstPage ? 1 : 2), this._model.pageCount), 0);
            this.jumpToPage(previousPage);
        }

        public getCurrentPageRotation() {
            return this._rotation;
        }

        public getCurrentPageZoom() {
            var scaling = this.getCurrentPageScaling();

            if (scaling !== -1) {
                return this._pageController.viewport.scale * scaling;
            }

            return this._pageController.viewport.scale;
        }

        public setCurrentPageZoom(zoom:number) {
            if (typeof this._pageController == "undefined") {
                return;
            }
            var scaling = this.getCurrentPageScaling();
            this._pageController.viewport.scale = zoom / scaling;
        }


        public getCurrentPageScaling() {
            if (typeof this._model == "undefined") {
                return -1;
            }
            var pageNumber = this.getCurrentPage();
            if (pageNumber != -1 && this._model.children.has(pageNumber)) {
                var page = this._model.children.get(pageNumber);
                var pageArea = this._pageController.getPageAreaInformation(page);
                if (typeof pageArea != "undefined") {
                    return pageArea.scale;
                }
            }
            return -1;
        }

        public setCurrentPositionInPage(pos:Position2D):void {
            var vpRect = this._pageController.viewport.asRectInArea();
            var page = this.getCurrentPage();
            var middle = this.getImageMiddle(page);
            var pageSize = this._pageDimension;
            var pagePos = new Position2D(middle.x - (pageSize.width / 2), middle.y - (pageSize.height / 2));
            this._pageController.viewport.position = new Position2D(pagePos.x + pos.x + (vpRect.size.width / 2), pagePos.y + pos.y + (vpRect.size.height / 2));
        }

    }

}