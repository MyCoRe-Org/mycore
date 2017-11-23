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

/// <reference path="../../components/model/AbstractPage.ts" />

namespace mycore.viewer.widgets.canvas {
    export class PageLayout {

        public init(model:OrderedListModel, pageController:PageController, pageDimension:Size2D, horizontalScrollbar:Scrollbar, verticalScrollbar:Scrollbar, pageLoader:(number) => void) {
            this._model = model;
            this._pageController = pageController;
            this._pageDimension = pageDimension;
            this._originalPageDimension = pageDimension;
            this._horizontalScrollbar = horizontalScrollbar;
            this._verticalScrollbar = verticalScrollbar;
            this._pageLoader = pageLoader;
        }

        public _originalPageDimension:Size2D;
        public _model:OrderedListModel;
        public _pageController:PageController;
        public _pageDimension:Size2D;
        public _horizontalScrollbar:Scrollbar;
        public _verticalScrollbar:Scrollbar;
        public _pageLoader:(number) => void;

        public _insertedPages:Array<number> = new Array();

        public getPageController():widgets.canvas.PageController {
            return this._pageController;
        }

        /**
         * Default implementation to update a page.
         * @param order the order number of the page.
         */
        public updatePage(order) {
            var shouldBeInserted = this.checkShouldBeInserted(order);
            var isInserted = this.isImageInserted(order);

            if (shouldBeInserted && !isInserted && this._model.children.has(order)) {
                var page = this._model.children.get(order);
                this._insertedPages.push(order);
                this._pageController.addPage(page, this.calculatePageAreaInformation(order));
            }

            if (!shouldBeInserted && isInserted && this._model.children.has(order)) {
                var page = this._model.children.get(order);
                this._insertedPages.splice(this._insertedPages.indexOf(order));
                this._pageController.removePage(page);
            }

        }

        public getRealPageDimension(pageNumber:number):Size2D {
            if (pageNumber != -1 && this._model.children.has(pageNumber)) {
                var page = this._model.children.get(pageNumber);

                var pageArea = this._pageController.getPageAreaInformation(page);
                if (typeof pageArea != "undefined") {
                    return page.size.scale(pageArea.scale).getRotated(this.getCurrentPageRotation());
                }
            }
            return this._pageDimension.getRotated(this.getCurrentPageRotation());
        }

        /**
         * Should update all needed pages!
         */
        public syncronizePages():void {
            throw "should be implemented";
        }


        /**
         * Should set the PageController to a clear state for a other PageLayout
         */
        public clear():void {
            throw "should be implemented";
        }

        /**
         * Checks if a page should be inserted.
         * @param order the order of the page to check
         */
        public
        checkShouldBeInserted(order:number):Boolean {
            throw "should be implemented";
        }

        /**
         * Should calculate the PageAreaInformation for a specific page
         * @param order the order of the page
         */
        public calculatePageAreaInformation(order:number):PageAreaInformation {
            throw "should be implemented";
        }

        /**
         *Gets the Position of a image in the specific layout
         * @param order  the order of the page
         */
        public getImageMiddle(order:number):Position2D {
            throw "should be implemented";
        }

        /**
         * Should fit the current page to the screen
         */
        public fitToScreen():void {
            throw "should be implemented";
        }

        /**
         * Should fit the current page to the width of the screen
         */
        public fitToWidth(attop:boolean = false):void {
            throw "should be implemented";
        }

        /**
         * Should return the current page
         */
        public getCurrentPage():number {
            throw "should be implemented";
        }

        /**
         * Should jump to a specific page in the layout
         * @param order  the order of the page
         */
        public jumpToPage(order:number):void {
            throw "should be implemented";
        }

        public isImageInserted(order:number) {
            return this._model.children.has(order) && (this._pageController.getPages().indexOf(this._model.children.get(order)) != -1);
        }

        /**
         * This method will be called if the user scrolls in the scrollbarElement
         */
        public scrollhandler():void {
            throw "should be implemented";
        }

        /**
         * This method will be called if the pages or the viewport should be rotated. The behavior depends on the layout.
         * @param deg the new rotation in degree
         */
        public rotate(deg:number):void {
            throw "should be implemented";
        }

        /**
         * Should return the label key for i18n which should be used to display a label in the Toolbar.
         */
        public getLabelKey():string {
            throw "should be implemented";
        }

        /**
         * Should return the Part of the viewport which should be rendered as the viewport
         */
        public getCurrentOverview():Rect {
            throw "should be implemented";
        }


        public next():void {
            throw "should be implemented";
        }

        public previous():void {
            throw "should be implemented";
        }

        public getCurrentPageRotation():number {
            throw "should be implemented";
        }

        public setCurrentPageZoom(zoom:number):void {
            throw "should be implemented";
        }

        public getCurrentPageZoom():number {
            throw "should be implemented";
        }

        public getCurrentPositionInPage():Position2D {
            let positionInArea = this._pageController.viewport.asRectInArea().pos;
            let page = this.getCurrentPage();
            let pageSize = this._pageDimension;
            let middle = this.getImageMiddle( page );
            let x = positionInArea.x - ( middle.x - ( pageSize.width / 2 ) );
            let y= positionInArea.y - ( middle.y - ( pageSize.height / 2 ) );
            return new Position2D(x, y);
        }

        public setCurrentPositionInPage(pos:Position2D):void {
            throw "should be implemented";
        }

        /**
         * Converts the given position to the area position.
         */
        public getPositionInArea(windowPosition:Position2D):Position2D {
            let viewport = this._pageController.viewport;
            let viewRect = viewport.asRectInArea();
            let areaX = viewRect.pos.x + (viewRect.size.width * (windowPosition.x / viewport.size.width));
            let areaY = viewRect.pos.y + (viewRect.size.height * (windowPosition.y / viewport.size.height));
            return new Position2D(areaX, areaY);
        }

        public getPageHitInfo(windowPosition:Position2D):PageHitInfo {
            let viewport = this._pageController.viewport;
            let pageArea = this._pageController.getPageArea();
            let positionInArea = this.getPositionInArea(windowPosition);

            for(let page of pageArea.getPagesInViewport(viewport)) {
                var structureImage = this._model.hrefImageMap.get(page.id);
                if(structureImage == null) {
                    continue;
                }
                var info:PageAreaInformation = pageArea.getPageInformation(page);
                var realPageDimension = page.size.getRotated(info.rotation).scale(info.scale);
                var pageRect = new Rect(new Position2D(
                            info.position.x - (realPageDimension.width / 2),
                            info.position.y - (realPageDimension.height / 2)),
                    realPageDimension);
                if(pageRect.intersects(positionInArea)) {
                    var r = pageRect.flip(info.rotation);
                    var p1 = r.pos.rotate(info.rotation);
                    var p2 = positionInArea.rotate(info.rotation);
                    var hit = new Position2D(Math.abs(p2.x - p1.x), Math.abs(p2.y - p1.y));
                    return {
                        id: page.id,
                        order: structureImage.order,
                        pageAreaInformation: info,
                        hit: hit
                    }
                }
            }
            return {
                id: null,
                order: null,
                pageAreaInformation: null,
                hit: null
            };
        }
    }

    export interface OrderedListModel {
        children: MyCoReMap<number, model.AbstractPage>;
        hrefImageMap: MyCoReMap<string, model.StructureImage>;
        pageCount: number;
    }

    export interface PageHitInfo {
        id: string,
        order: number,
        pageAreaInformation:PageAreaInformation,
        hit: Position2D
    }

}
