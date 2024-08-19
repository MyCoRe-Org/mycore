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


import {MyCoReMap, Position2D, Rect} from "../../Utils";
import {PageLayout} from "./PageLayout";
import {PageAreaInformation} from "./model/PageArea";

export class SinglePageLayout extends PageLayout {
    private _paiCache = new MyCoReMap<number, PageAreaInformation>();
    private _rotation: number = 0;

    public syncronizePages(): void {
        const vp = this._pageController.viewport;
        const pageSizeWithSpace = this.getPageHeightWithSpace();
        const rowCount = this._model.pageCount;

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

        const vpSizeInArea = vp.size.height / vp.scale;
        const yStart = vp.position.y - (vpSizeInArea / 2);
        const yEnd = vp.position.y + (vpSizeInArea / 2);
        const yStartOrder = Math.floor(yStart / pageSizeWithSpace);
        const yEndOrder = Math.ceil(yEnd / pageSizeWithSpace);

        const pagesToCheck = this._insertedPages.slice(0);

        for (let y = yStartOrder; y <= yEndOrder; y++) {
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

    public clear(): void {
        const pages = this._pageController.getPages().slice(0);
        this._insertedPages.splice(0, this._insertedPages.length);
        pages.forEach((p) => {
            this._pageController.removePage(p)
        });
    }

    public fitToScreen(): void {
        const vp = this._pageController.viewport;
        if (vp.size.width != 0 && vp.size.height != 0) {
            const vpRotated = vp.size;//.getRotated(vp.rotation);
            const realPageDimension = this.getRealPageDimension(this.getCurrentPage());

            vp.scale = Math.min(vpRotated.width / realPageDimension.width, vpRotated.height / realPageDimension.height);
            const imgMiddle = this.getImageMiddle(this.getCurrentPage());
            vp.position = new Position2D(0, imgMiddle.y);
        }
    }

    public calculatePageAreaInformation(order: number) {
        const imgSize = this._model.children.get(order).size;
        const pai = new PageAreaInformation();

        pai.position = this.getImageMiddle(order);
        pai.scale = Math.min(this._originalPageDimension.width / imgSize.width, this._originalPageDimension.height / imgSize.height);
        pai.rotation = this._rotation;

        this._paiCache.set(order, pai);

        return pai;
    }

    public checkShouldBeInserted(order: number) {
        const vpRect = this._pageController.viewport.asRectInArea();
        const imagePos = this.getImageMiddle(order);
        const imageRect = new Rect(new Position2D(imagePos.x - (this._pageDimension.width / 2), imagePos.y - (this._pageDimension.height / 2)), this._pageDimension);
        const overviewRect = this.getCurrentOverview();
        return vpRect.getIntersection(imageRect) != null || overviewRect.getIntersection(imageRect) != null;
    }

    public fitToWidth(attop: boolean = false): void {
        const middle = this.getImageMiddle(this.getCurrentPage());

        const realPageDimension = this.getRealPageDimension(this.getCurrentPage());
        this._pageController.viewport.scale = this._pageController.viewport.size.width / (realPageDimension.width);

        let correctedY = middle.y;
        if (attop) {
            const vp = this._pageController.viewport;
            const scaledViewport = vp.size.scale(1 / vp.scale);
            correctedY = (correctedY - realPageDimension.height / 2) + scaledViewport.height / 2;
        }

        this._pageController.viewport.position = new Position2D(0, correctedY);
    }

    public getCurrentPage(): number {
        if (typeof this._pageController == "undefined") {
            return 0;
        }
        return Math.ceil(this._pageController.viewport.position.y / this.getPageHeightWithSpace());
    }

    public jumpToPage(order: number): void {
        const middleOfImage = this.getImageMiddle(order);
        const pageRect = new Rect(new Position2D(middleOfImage.x - (this._pageDimension.width / 2), middleOfImage.y - (this._pageDimension.height / 2)), this._pageDimension);
        this._pageController.viewport.setRect(pageRect);
    }

    private getPageHeightWithSpace() {
        return this._pageDimension.height + (this._pageDimension.height / 10);
    }

    public getImageMiddle(order: number) {
        const pageSizeWithSpace = this.getPageHeightWithSpace();
        const middle = order * pageSizeWithSpace - (this._pageDimension.height / 2);
        return new Position2D(0, middle);
    }

    public scrollhandler() {
        const vp = this._pageController.viewport;
        const scrollPos = new Position2D(this._horizontalScrollbar.position, this._verticalScrollbar.position);

        const xPos = scrollPos.x + (vp.size.width / vp.scale / 2) - (this._pageDimension.width / 2);

        vp.position = new Position2D(xPos, scrollPos.y + (vp.size.height / vp.scale / 2));
    }

    public maximalPageScale = 4;

    private correctViewport(): void {
        const vp = this._pageController.viewport;
        const pageScaling = this.getCurrentPageScaling();

        if (pageScaling != -1) {
            const minWidthScale = this._pageController.viewport.size.width / this._pageDimension.width;
            const minScale = Math.min(this._pageController.viewport.size.height / (this._pageDimension.height * 2), minWidthScale);

            if (vp.scale < minScale) {
                vp.stopAnimation();
                vp.scale = minScale;
            }

            const completeScale = vp.scale * pageScaling;
            if (completeScale > this.maximalPageScale) {
                vp.stopAnimation();
                vp.scale = this.maximalPageScale / pageScaling;
            }
        }

        const scaledViewport = vp.size.scale(1 / vp.scale);
        const minY = Math.ceil(vp.asRectInArea().size.height / 2);
        const maxY = Math.ceil(this._model.pageCount * this.getPageHeightWithSpace() - Math.floor(vp.asRectInArea().size.height / 2));
        const correctedY = Math.min(Math.max(vp.position.y, minY), maxY);

        if (scaledViewport.width > (this._pageDimension.width)) {
            const corrected = new Position2D(0, correctedY);
            if (!vp.position.equals(corrected)) {
                vp.position = corrected;
            }
        } else {
            const minimalX = (-this._pageDimension.width / 2) + scaledViewport.width / 2;
            const maximalX = (this._pageDimension.width / 2) - scaledViewport.width / 2;
            const correctedX = Math.max(minimalX, Math.min(maximalX, vp.position.x));
            const corrected = new Position2D(correctedX, correctedY);
            if (!vp.position.equals(corrected)) {
                vp.position = corrected;
            }
        }
    }

    public rotate(deg: number): void {
        this._rotation = deg;
        const currentPage = this.getCurrentPage();
        this._pageDimension = this._originalPageDimension.getRotated(deg);

        this.clear();
        this.syncronizePages();
        this.jumpToPage(currentPage);
    }

    public getLabelKey(): string {
        return "singlePageLayout";
    }

    /**
     * Should return the Part of the viewport which should be rendered as the viewport
     */
    public getCurrentOverview(): Rect {
        const pageSize = this.getRealPageDimension(this.getCurrentPage());
        const pagePosition = this.getImageMiddle(this.getCurrentPage());
        return new Rect(new Position2D(pagePosition.x - (pageSize.width / 2), pagePosition.y - (pageSize.height / 2)), pageSize);
    }

    public next(): void {
        const page = this.getCurrentPage();
        const nextPage = Math.max(Math.min(page + 1, this._model.pageCount), 0);
        const scale = this._pageController.viewport.scale;
        const pos = this.getCurrentPositionInPage();
        this.jumpToPage(nextPage);
        this._pageController.viewport.scale = scale;
        this.setCurrentPositionInPage(pos);
    }

    public previous(): void {
        const page = this.getCurrentPage();
        const previousPage = Math.max(Math.min(page - 1, this._model.pageCount), 0);
        const scale = this._pageController.viewport.scale;
        const pos = this.getCurrentPositionInPage();
        this.jumpToPage(previousPage);
        this._pageController.viewport.scale = scale;
        this.setCurrentPositionInPage(pos);
    }

    public getCurrentPageRotation() {
        return this._rotation;
    }

    public getCurrentPageZoom() {
        if (typeof this._pageController == "undefined") {
            return;
        }
        const scaling = this.getCurrentPageScaling();

        if (scaling !== -1) {
            return this._pageController.viewport.scale * scaling;
        }

        return this._pageController.viewport.scale;
    }

    public setCurrentPageZoom(zoom: number) {
        if (typeof this._pageController == "undefined") {
            return;
        }
        const scaling = this.getCurrentPageScaling();
        this._pageController.viewport.scale = zoom / scaling;
    }

    public getCurrentPageScaling() {
        if (typeof this._model == "undefined") {
            return -1;
        }
        const pageNumber = this.getCurrentPage();
        if (pageNumber != -1 && this._model.children.has(pageNumber)) {
            const page = this._model.children.get(pageNumber);
            const pageArea = this._pageController.getPageAreaInformation(page);
            if (typeof pageArea != "undefined") {
                return pageArea.scale;
            }
        }
        return -1;
    }

    public setCurrentPositionInPage(pos: Position2D): void {
        const vpRect = this._pageController.viewport.asRectInArea();
        const page = this.getCurrentPage();
        const middle = this.getImageMiddle(page);
        const pageSize = this._pageDimension;
        const pagePos = new Position2D(middle.x - (pageSize.width / 2), middle.y - (pageSize.height / 2));
        this._pageController.viewport.position = new Position2D(pagePos.x + pos.x + (vpRect.size.width / 2), pagePos.y + pos.y + (vpRect.size.height / 2));
    }

}


