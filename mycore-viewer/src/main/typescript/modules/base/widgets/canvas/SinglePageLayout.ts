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
    export class SinglePageLayout extends PageLayout {
        private _paiCache = new MyCoReMap<number, PageAreaInformation>();
        private _rotation:number = 0;

        public syncronizePages():void {
            var vp = this._pageController.viewport;
            var pageSizeWithSpace = this.getPageHeightWithSpace();
            var rowCount = this._model.pageCount;

            this.correctViewport();
            if (typeof this._horizontalScrollbar != "undefined" && this._horizontalScrollbar != null) {
                this._horizontalScrollbar.areaSize = this._pageDimension.width;
                this._horizontalScrollbar.viewSize = this._pageController.viewport.size.width / this._pageController.viewport.scale;
                this._horizontalScrollbar.position = vp.position.x - (vp.size.width / vp.scale / 2) + (this._pageDimension.width / 2);
            }

            if (typeof this._verticalScrollbar != "undefined" && this._verticalScrollbar != null) {
                this._verticalScrollbar.areaSize = rowCount * this.getPageHeightWithSpace();
                this._verticalScrollbar.viewSize = this._pageController.viewport.size.height / this._pageController.viewport.scale;
                this._verticalScrollbar.position = vp.position.y - (vp.size.height / vp.scale / 2);
            }

            var vpSizeInArea = vp.size.height / vp.scale;
            var yStart = vp.position.y - (vpSizeInArea / 2);
            var yEnd = vp.position.y + (vpSizeInArea / 2);
            var yStartOrder = Math.floor(yStart / pageSizeWithSpace);
            var yEndOrder = Math.ceil(yEnd / pageSizeWithSpace);

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
                var vpRotated = vp.size;//.getRotated(vp.rotation);
                var realPageDimension = this.getRealPageDimension(this.getCurrentPage());

                vp.scale = Math.min(vpRotated.width / realPageDimension.width, vpRotated.height / realPageDimension.height);
                var imgMiddle = this.getImageMiddle(this.getCurrentPage());
                vp.position = new Position2D(0, imgMiddle.y);
            }
        }

        public calculatePageAreaInformation(order:number) {
            var imgSize = this._model.children.get(order).size;
            var pai = new widgets.canvas.PageAreaInformation();

            pai.position = this.getImageMiddle(order);
            pai.scale = Math.min(this._originalPageDimension.width / imgSize.width, this._originalPageDimension.height / imgSize.height);
            pai.rotation = this._rotation;

            this._paiCache.set(order, pai);

            return pai;
        }

        public checkShouldBeInserted(order:number) {
            var vpRect = this._pageController.viewport.asRectInArea();
            var imagePos = this.getImageMiddle(order);
            var imageRect = new Rect(new Position2D(imagePos.x - (this._pageDimension.width / 2), imagePos.y - (this._pageDimension.height / 2)), this._pageDimension);
            var overviewRect = this.getCurrentOverview();
            return vpRect.getIntersection(imageRect) != null || overviewRect.getIntersection(imageRect) != null;
        }

        public fitToWidth(attop:boolean = false):void {
            var middle = this.getImageMiddle(this.getCurrentPage());

            var realPageDimension = this.getRealPageDimension(this.getCurrentPage());
            this._pageController.viewport.scale = this._pageController.viewport.size.width / (realPageDimension.width);

            var correctedY = middle.y;
            if (attop) {
                var vp = this._pageController.viewport;
                var scaledViewport = vp.size.scale(1 / vp.scale);
                correctedY = (correctedY - realPageDimension.height / 2) + scaledViewport.height / 2;
            }

            this._pageController.viewport.position = new Position2D(0, correctedY);
        }

        public getCurrentPage():number {
            if (typeof this._pageController == "undefined") {
                return 0;
            }
            return Math.ceil(this._pageController.viewport.position.y / this.getPageHeightWithSpace());
        }

        public jumpToPage(order:number):void {
            var middleOfImage = this.getImageMiddle(order);
            var pageRect = new Rect(new Position2D(middleOfImage.x - (this._pageDimension.width / 2), middleOfImage.y - (this._pageDimension.height / 2)), this._pageDimension);
            this._pageController.viewport.setRect(pageRect);
        }

        private getPageHeightWithSpace() {
            return this._pageDimension.height + (this._pageDimension.height / 10);
        }

        public getImageMiddle(order:number) {
            var pageSizeWithSpace = this.getPageHeightWithSpace();
            var middle = order * pageSizeWithSpace - (this._pageDimension.height / 2);
            return new Position2D(0, middle);
        }

        public scrollhandler() {
            var vp = this._pageController.viewport;
            var scrollPos = new Position2D(this._horizontalScrollbar.position, this._verticalScrollbar.position);

            var xPos = scrollPos.x + (vp.size.width / vp.scale / 2) - (this._pageDimension.width / 2);

            vp.position = new Position2D(xPos, scrollPos.y + (vp.size.height / vp.scale / 2));
        }

        private correctViewport():void {
            let vp = this._pageController.viewport;
            let pageScaling = this.getCurrentPageScaling();

            if (pageScaling != -1) {
                let minWidthScale = this._pageController.viewport.size.width / this._pageDimension.width;
                let minScale = Math.min(this._pageController.viewport.size.height / (this._pageDimension.height * 2), minWidthScale);

                if (vp.scale < minScale) {
                    vp.stopAnimation();
                    vp.scale = minScale;
                }

                let completeScale = vp.scale * pageScaling;
                if (completeScale > 4) {
                    vp.stopAnimation();
                    vp.scale = 4 / pageScaling;
                }
            }

            let scaledViewport = vp.size.scale(1 / vp.scale);
            let minY = Math.ceil(vp.asRectInArea().size.height / 2);
            let maxY = Math.ceil(this._model.pageCount * this.getPageHeightWithSpace() - Math.floor(vp.asRectInArea().size.height / 2));
            let correctedY = Math.min(Math.max(vp.position.y, minY), maxY);

            if (scaledViewport.width > (this._pageDimension.width)) {
                let corrected = new Position2D(0, correctedY);
                if (!vp.position.equals(corrected)) {
                    vp.position = corrected;
                }
            } else {
                let minimalX = (-this._pageDimension.width / 2) + scaledViewport.width / 2;
                let maximalX = (this._pageDimension.width / 2) - scaledViewport.width / 2;
                let correctedX = Math.max(minimalX, Math.min(maximalX, vp.position.x));
                let corrected = new Position2D(correctedX, correctedY);
                if (!vp.position.equals(corrected)) {
                    vp.position = corrected;
                }
            }
        }

        public rotate(deg:number):void {
            this._rotation = deg;
            var currentPage = this.getCurrentPage();
            this._pageDimension = this._originalPageDimension.getRotated(deg);

            this.clear();
            this.syncronizePages();
            this.jumpToPage(currentPage);
        }

        public getLabelKey():string {
            return "singlePageLayout";
        }

        /**
         * Should return the Part of the viewport which should be rendered as the viewport
         */
        public getCurrentOverview():Rect {
            var pageSize = this.getRealPageDimension(this.getCurrentPage());
            var pagePosition = this.getImageMiddle(this.getCurrentPage());
            return new Rect(new Position2D(pagePosition.x - (pageSize.width / 2), pagePosition.y - (pageSize.height / 2)), pageSize);
        }

        public next():void {
            var page = this.getCurrentPage();
            var nextPage = Math.max(Math.min(page + 1, this._model.pageCount), 0);
            var scale = this._pageController.viewport.scale;
            var pos = this.getCurrentPositionInPage();
            this.jumpToPage(nextPage);
            this._pageController.viewport.scale = scale;
            this.setCurrentPositionInPage(pos);
        }

        public previous():void {
            var page = this.getCurrentPage();
            var previousPage = Math.max(Math.min(page - 1, this._model.pageCount), 0);
            var scale = this._pageController.viewport.scale;
            var pos = this.getCurrentPositionInPage();
            this.jumpToPage(previousPage);
            this._pageController.viewport.scale = scale;
            this.setCurrentPositionInPage(pos);
        }

        public  getCurrentPageRotation() {
            return this._rotation;
        }

        public getCurrentPageZoom() {
            if (typeof this._pageController == "undefined") {
                return;
            }
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
