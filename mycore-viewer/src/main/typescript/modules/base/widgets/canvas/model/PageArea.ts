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


import {MyCoReMap, Position2D, Rect, ViewerProperty, ViewerPropertyObserver} from "../../../Utils";
import {AbstractPage} from "../../../components/model/AbstractPage";
import {Viewport} from "../viewport/Viewport";

export class PageArea implements ViewerPropertyObserver<any> {


        private _pages:Array<AbstractPage> = new Array<AbstractPage>();
        private _pageAreaInformationMap:MyCoReMap<AbstractPage, PageAreaInformation> = new MyCoReMap<AbstractPage, PageAreaInformation>();
        private _updateCallback:()=>void = null;

        public get updateCallback():()=>void {
            if (this._updateCallback == null) {
                return () => {
                };
            }
            return  this._updateCallback;
        }

        public set updateCallback(callback:()=>void) {
            this._updateCallback = callback;
        }

        public addPage(page:AbstractPage, info:PageAreaInformation):void {
            this.setPageAreaInformation(page, info);
            this._pages.push(page);
            this.registerPageAreaInformationEvents(info);
            this.updateCallback();
        }

        public removePage(page:AbstractPage):void {
            this._pages.splice(this._pages.indexOf(page), 1);
            const pageInformation = this._pageAreaInformationMap.get(page);
            this._pageAreaInformationMap.remove(page);
            this.unregisterPageAreaInformationEvents(pageInformation);
            this.updateCallback();
        }

        public propertyChanged(_old:ViewerProperty<any>, _new:ViewerProperty<any>) {
            this._updateCallback();
        }

        public setPageAreaInformation(page:AbstractPage, info:PageAreaInformation):void {
            this._pageAreaInformationMap.set(page, info);
        }

        public getPages():Array<AbstractPage> {
            return this._pages;
        }

        public getPagesInViewport(viewPort:Viewport):Array<AbstractPage> {
            const pages = this._pages;
            const pagesInViewport = new Array<AbstractPage>();
            pages.forEach((page) => {
                if (this.pageIntersectViewport(page, viewPort)) {
                    pagesInViewport.push(page);
                }
            });
            return pagesInViewport;
        }

        public getPageInformation(page:AbstractPage) {
            return this._pageAreaInformationMap.get(page);
        }

        private pageIntersectViewport(page:AbstractPage, viewPort:Viewport) {
            const areaInformation = this._pageAreaInformationMap.get(page);
            const pageDimension = page.size.getRotated(areaInformation.rotation).scale(areaInformation.scale);
            const upperLeftPosition = new Position2D(areaInformation.position.x - pageDimension.width / 2, areaInformation.position.y - pageDimension.height / 2);

            // this is the real rectangle of the page. With upper left corner and the size of the page[rotated])
            const pageRect = new Rect(upperLeftPosition, pageDimension);
            const viewPortRect = viewPort.asRectInArea();

            return pageRect.getIntersection(viewPortRect) != null;
        }


        private registerPageAreaInformationEvents(info:PageAreaInformation) {
            info.positionProperty.addObserver(this);
            info.rotationProperty.addObserver(this);
            info.scaleProperty.addObserver(this);
        }

        private unregisterPageAreaInformationEvents(info:PageAreaInformation) {
            info.positionProperty.removeObserver(this);
            info.rotationProperty.removeObserver(this);
            info.scaleProperty.removeObserver(this);
        }



    }

export class PageAreaInformation {

        private _positionProperty:ViewerProperty<Position2D> = new ViewerProperty(this, "position", new Position2D(0, 0));

        get positionProperty():ViewerProperty<Position2D> {
            return this._positionProperty;
        }

        private _scaleProperty:ViewerProperty<number> = new ViewerProperty(this, "scale", 1);

        get scaleProperty():ViewerProperty<number> {
            return this._scaleProperty;
        }

        private _rotationProperty:ViewerProperty<number> = new ViewerProperty(this, "rotation", 0);

        get rotationProperty():ViewerProperty<number> {
            return this._rotationProperty;
        }

        get rotation():number {
            return this._rotationProperty.value;
        }

        set rotation(value:number) {
            this.rotationProperty.value = value;
        }

        get scale():number {
            return this.scaleProperty.value;
        }

        set scale(value:number) {
            this.scaleProperty.value = value;
        }

        get position():Position2D {
            return this.positionProperty.value;
        }

        set position(value:Position2D) {
            this.positionProperty.value = value;
        }
}



