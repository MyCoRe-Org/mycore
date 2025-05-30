/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import { PageLayout } from "../../../base/widgets/canvas/PageLayout";
import { PageAreaInformation } from "../../../base/widgets/canvas/model/PageArea";
import { Position2D, Rect, Size2D } from "../../../base/Utils";

export class DoublePageLayout extends PageLayout {

  public get relocated(): boolean {
    return false;
  }

  private _rotation: number = 0;
  private _currentPage: number = 0;

  public syncronizePages(): void {
    const vp = this._pageController.viewport;
    const pageSizeWithSpace = this.getPageHeightWithSpace();
    const rowCount = this._model.pageCount / 2;

    this.correctViewport();

    const widthMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 1 : 2;
    const widthDivisor = (widthMultiplicator == 1) ? 2 : 1;

    this._horizontalScrollbar.viewSize = this._pageController.viewport.size.width / this._pageController.viewport.scale;
    this._verticalScrollbar.viewSize = this._pageController.viewport.size.height / this._pageController.viewport.scale;

    this._horizontalScrollbar.areaSize = this._pageDimension.width * widthMultiplicator;
    this._verticalScrollbar.areaSize = rowCount * this.getPageHeightWithSpace() + (this._verticalScrollbar.viewSize / 2) - this.getPageHeightSpace();

    this._horizontalScrollbar.position = vp.position.x - (vp.size.width / vp.scale / 2) + (this._pageDimension.width / widthDivisor);
    this._verticalScrollbar.position = vp.position.y - (vp.size.height / vp.scale / 2);
    const pagesPerLine = 2;

    const vpSizeInArea = vp.size.height / vp.scale;
    const yStart = vp.position.y - (vpSizeInArea / 2);
    const yEnd = vp.position.y + (vpSizeInArea / 2);
    const yStartOrder = Math.floor(yStart / pageSizeWithSpace) * pagesPerLine;
    const yEndOrder = Math.ceil(yEnd / pageSizeWithSpace) * pagesPerLine + (this.relocated ? 1 : 0);

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
      const dpRect = this.getDoublePageRect(this.getCurrentPage());
      vp.setRect(dpRect);
    }
  }

  public calculatePageAreaInformation(order: number) {
    const imgSize = this._model.children.get(order).size;
    const pai = new PageAreaInformation();
    const pr = this.getPageRect(order);
    const page = this._model.children.get(order);

    pai.scale = Math.min(this._originalPageDimension.width / imgSize.width, this._originalPageDimension.height / imgSize.height);

    const realPageDimension = page.size.scale(pai.scale);//.getRotated(this._rotation);
    pai.position = new Position2D(pr.pos.x + realPageDimension.width / 2, pr.pos.y + realPageDimension.height / 2);
    pai.rotation = this._rotation;

    return pai;
  }

  public checkShouldBeInserted(order: number) {
    const vpRect = this._pageController.viewport.asRectInArea();
    const imagePos = this.getImageMiddle(order);
    const imageRect = new Rect(new Position2D(imagePos.x - (this._pageDimension.width), imagePos.y - (this._pageDimension.height / 2)), new Size2D(this._pageDimension.width * 2, this._pageDimension.height));
    return vpRect.getIntersection(imageRect) != null || this.getCurrentOverview().getIntersection(imageRect) != null;
  }

  public fitToWidth(attop: boolean = false): void {
    const pr = this.getDoublePageRect(this.getCurrentPage());
    this._pageController.viewport.position = pr.getMiddlePoint();
    this._pageController.viewport.scale = this._pageController.viewport.size.width / pr.size.width
  }

  public getCurrentPage(): number {
    const pagesPerLine = 2;
    const vp = this._pageController.viewport;
    const pageSizeWithSpace = this.getPageHeightWithSpace();
    const rowCount = this._model.pageCount / 2;
    const vpSizeInArea = vp.size.height / vp.scale;
    const yStart = vp.position.y - (vpSizeInArea / 2);
    const yEnd = vp.position.y + (vpSizeInArea / 2);
    const yStartOrder = Math.floor(yStart / pageSizeWithSpace) * pagesPerLine;
    const yEndOrder = Math.ceil(yEnd / pageSizeWithSpace) * pagesPerLine + (this.relocated ? 1 : 0);

    let maxPage = -1;
    let maxCount = -1;
    let maxIsMiddle = false;
    const vpRect = vp.asRectInArea();
    for (let y = yStartOrder; y <= yEndOrder; y++) {
      const curRect = this.getPageRect(y).getIntersection(vpRect);
      if (curRect != null && (!maxIsMiddle || y == this._currentPage)) {
        const curCount = curRect.size.getSurface();
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

  public jumpToPage(order: number): void {
    const middle = this.getImageMiddle(order);
    //this._pageController.viewport.position = new Position2D(0, middle.y);
    this._currentPage = order;
    this._pageController.viewport.setRect(this.getPageRect(order))
  }

  private getPageHeightSpace() {
    return ((this._pageDimension.height / 100) * 10);
  }

  private getPageHeightWithSpace() {
    const rotationMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 2 : 1;
    return (this._pageDimension.height * rotationMultiplicator) + this.getPageHeightSpace();
  }

  public getImageMiddle(order: number) {
    const pageRect = this.getPageRect(order);
    return pageRect.getMiddlePoint();
  }

  public maximalPageScale = 4;

  private correctViewport(): void {
    const vp = this._pageController.viewport;
    const widthMultiplicator = (this._rotation == 90 || this._rotation == 270) ? 1 : 2;

    const pageScaling = this.getCurrentPageScaling();
    if (pageScaling != -1) {
      const minWidthScale = this._pageController.viewport.size.width / (this._pageDimension.width * widthMultiplicator);
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
    const minY = 1;
    const maxY = (this._rotation === 90 || this._rotation === 270 ?
      (((this.relocated ? 1 : 0) + this._model.pageCount) / 2) * this.getPageHeightWithSpace() :
      Math.ceil(((this.relocated ? 1 : 0) + this._model.pageCount) / 2) * this.getPageHeightWithSpace());
    const correctedY = Math.min(Math.max(vp.position.y, minY), maxY);
    if (scaledViewport.width > (this._pageDimension.width * widthMultiplicator)) {
      const corrected = new Position2D(0, correctedY);
      if (!vp.position.equals(corrected)) {
        vp.position = new Position2D(0, correctedY);
      }
    } else {
      const minimalX = -(this._pageDimension.width / (2 / widthMultiplicator)) + scaledViewport.width / 2;
      const maximalX = (this._pageDimension.width / (2 / widthMultiplicator)) - scaledViewport.width / 2;
      const correctedX = Math.max(minimalX, Math.min(maximalX, vp.position.x));
      const corrected = new Position2D(correctedX, correctedY);
      if (!vp.position.equals(corrected)) {
        vp.position = corrected;
      }
    }
  }

  public scrollhandler() {
    if (this._pageController.viewport.currentAnimation == null) {
      const widthDivisor = (this._rotation == 90 || this._rotation == 270) ? 2 : 1;
      const vp = this._pageController.viewport;
      const scrollPos = new Position2D(this._horizontalScrollbar.position, this._verticalScrollbar.position);

      const xPos = scrollPos.x + (vp.size.width / vp.scale / 2) - (this._pageDimension.width / widthDivisor);

      vp.position = new Position2D(xPos, scrollPos.y + (vp.size.height / vp.scale / 2));
    }
  }

  public rotate(deg: number): void {
    const currentPage = this.getCurrentPage();
    this._pageDimension = this._originalPageDimension.getRotated(deg);
    this._rotation = deg;

    this.clear();
    this.syncronizePages();
    this.jumpToPage(currentPage);
  }

  public getLabelKey(): string {
    return "doublePageLayout";
  }

  public getCurrentOverview(): Rect {
    return this.getDoublePageRect(this.getCurrentPage());
  }

  public getDoublePageRect(order: number): Rect {
    const row = Math.floor(((order - 1) + (this.relocated ? 1 : 0)) / 2);
    const firstPage = (order - 1 + (this.relocated ? 1 : 0)) % 2 == 0;

    let start;
    if (firstPage) {
      start = order;
    } else {
      start = order - 1;
    }

    const p1Rect = this.getPageRect(start);
    const p2Rect = this.getPageRect(start + 1);

    return Rect.getBounding(p1Rect, p2Rect);
  }

  public getPageRect(order: number): Rect {
    const row = Math.floor(((order - 1) + (this.relocated ? 1 : 0)) / 2);
    const firstPage = (order - 1 + (this.relocated ? 1 : 0)) % 2 == 0;
    const pageDimension = this.getRealPageDimension(order);
    const yPos = (row * this.getPageHeightWithSpace());

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

  public next(): void {
    const page = this.getCurrentPage();
    const firstPage = (page - 1 + (this.relocated ? 1 : 0)) % 2 == 0 && (this._rotation == 0 || this._rotation == 180);
    const nextPage = Math.max(Math.min(page + (firstPage ? 2 : 1), this._model.pageCount), 0);
    this.jumpToPage(nextPage);
  }

  public previous(): void {
    const page = this.getCurrentPage();
    const firstPage = (page - 1 + (this.relocated ? 1 : 0)) % 2 == 0 && (this._rotation == 0 || this._rotation == 180);
    const previousPage = Math.max(Math.min(page - (firstPage ? 1 : 2), this._model.pageCount), 0);
    this.jumpToPage(previousPage);
  }

  public getCurrentPageRotation() {
    return this._rotation;
  }

  public getCurrentPageZoom() {
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
