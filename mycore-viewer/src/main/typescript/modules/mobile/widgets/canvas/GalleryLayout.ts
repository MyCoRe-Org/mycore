module mycore.viewer.widgets.canvas {
    export class GalleryLayout extends PageLayout {
        private _paiCache = new MyCoReMap<number, PageAreaInformation>();
        private _rotation:number = 0;

        public syncronizePages():void {
            var vp = this._pageController.viewport;
            var pageSizeWithSpace = this.getPageWidthWithSpace();
            var rowCount = this._model.pageCount;

            this.correctViewport();
            var vpWidthInArea = vp.size.width / vp.scale;
            var xStart = vp.position.x - (vpWidthInArea / 2);
            var xEnd = vp.position.x + (vpWidthInArea / 2);
            var xStartOrder = Math.floor(xStart / pageSizeWithSpace);
            var xEndOrder = Math.ceil(xEnd / pageSizeWithSpace);

            var currentPageOrder = Math.ceil(vp.position.x / pageSizeWithSpace);
            var pagesToCheck = this._insertedPages.slice(0);

            for (var x = xStartOrder; x <= xEndOrder; x++) {
                if (this._model.children.has(x) && pagesToCheck.indexOf(x) == -1) {
                    pagesToCheck.push(x);
                } else if (0 < x && x <= this._model.pageCount) {
                    this._pageLoader(x);
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
            var pageSizeWithSpace = this.getPageWidthWithSpace();
            if (vp.size.width != 0 && vp.size.height != 0) {
                var vpRotated = vp.size.getRotated(vp.rotation);
                vp.scale = Math.min(vpRotated.width / this._pageDimension.width, vpRotated.height / this._pageDimension.height);
                var imgMiddle = this.getImageMiddle(this.getCurrentPage());
                vp.position = new Position2D(imgMiddle.x, 0);
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
            var pos = this.getImageMiddle(order);
            var vpRect = this._pageController.viewport.asRectInArea();
            var imagePos = this.getImageMiddle(order);
            var imageRect = new Rect(new Position2D(imagePos.x - (this._pageDimension.width / 2), imagePos.y - (this._pageDimension.height / 2)), this._pageDimension);
            return vpRect.getIntersection(imageRect) != null;
        }

        public fitToWidth():void {
            var middle = this.getImageMiddle(this.getCurrentPage());
            this._pageController.viewport.scale = this._pageController.viewport.size.width / (this._pageDimension.width);
            this._pageController.viewport.position = new Position2D(0, middle.y);
        }

        public getCurrentPage():number {
            return Math.ceil(this._pageController.viewport.position.x / this.getPageWidthWithSpace());
        }

        public jumpToPage(order:number):void {
            var middleOfImage = this.getImageMiddle(order);
            var pageRect = new Rect(new Position2D(middleOfImage.x - (this._pageDimension.width / 2), middleOfImage.y - (this._pageDimension.height / 2)), this._pageDimension);
            this._pageController.viewport.setRect(pageRect);
        }

        private getPageWidthWithSpace() {
            return this._pageDimension.width + ((this._pageDimension.width / 100) * 10);
        }

        public getImageMiddle(order:number) {
            var pageSizeWithSpace = this.getPageWidthWithSpace();
            var middle = order * pageSizeWithSpace - (this._pageDimension.width / 2);
            return new Position2D(middle, 0);
        }

        public scrollhandler() {
        }

        private correctViewport():void {
            var vp = this._pageController.viewport;
            var scaledViewport = vp.size.scale(1 / vp.scale);
            var minX = 0;
            var maxX = this._model.pageCount * this.getPageWidthWithSpace();
            var correctedX = Math.min(Math.max(vp.position.x, minX), maxX);

            var vpRotated = vp.size.getRotated(vp.rotation);
            var minScale = Math.min(vpRotated.width / this._pageDimension.width, vpRotated.height / this._pageDimension.height);
            if (vp.scale < minScale) {
                vp.scale = minScale;
            }

            var minY = (-this._pageDimension.height / 2) + scaledViewport.height / 2;
            var maxY = (this._pageDimension.height / 2) - scaledViewport.height / 2;

            var correctedVp = new Position2D(correctedX, Math.min(maxY, Math.max(vp.position.y, minY)));
            if(!vp.position.equals(correctedVp)){
                vp.position = correctedVp;
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
            return "GalleryLayout";
        }

        public getCurrentPositionInPage():Position2D {
            var vpRect = this._pageController.viewport.asRectInArea();

            var page = this.getCurrentPage();
            var middle = this.getImageMiddle(page);
            var pageSize = this._pageDimension;

            return new Position2D(vpRect.pos.x - (middle.x - (pageSize.width / 2)), vpRect.pos.y - (middle.y - (pageSize.height / 2)));
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