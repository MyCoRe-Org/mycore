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

/// <reference path="../../components/model/TextContent.ts" />
namespace mycore.viewer.widgets.canvas {
    export class HtmlRenderer {
        constructor(private _vp:Viewport, private _area:PageArea, private _view:PageView) {
            this.htmlContainer = document.createElement("div");
            this.htmlContainer.setAttribute("class", "textContainer");
            var htmlElement = <HTMLElement>this._view.container[0];
            htmlElement.appendChild(this.htmlContainer);
        }

        private _addedPages:Array<model.AbstractPage> = new Array<model.AbstractPage>();
        public htmlContainer:HTMLElement;
        private _pageElementCache:MyCoReMap<model.AbstractPage, HTMLElement> = new MyCoReMap<model.AbstractPage, HTMLElement>();
        private _idPageMap = new MyCoReMap<string, model.AbstractPage>();
        private _addedContentMap = new MyCoReMap<model.AbstractPage, ViewerProperty<HTMLElement>>();

        public update() {
            var pagesInViewport = this._area.getPagesInViewport(this._vp);
            pagesInViewport.forEach((page:model.AbstractPage) => {
                if (!this._idPageMap.has(page.id)) {
                    this._idPageMap.set(page.id, page);
                }

                if (this._addedPages.indexOf(page) == -1) {
                    this.addPage(page);
                }

                if(!this._addedContentMap.has(page)){
                    if("getHTMLContent" in page){
                        let content = (<any>page).getHTMLContent();
                        this._addedContentMap.set(page, content);
                        let observer = {
                            propertyChanged : (_old: ViewerProperty<HTMLElement>, _new: ViewerProperty<HTMLElement>) => {
                                if (_new.value != null) {
                                    let htmlElement = this._pageElementCache.get(page);
                                    let root = htmlElement.querySelector("div div");
                                    root.innerHTML = "";
                                    root.appendChild(_new.value);
                                }
                            }
                        };
                        content.addObserver(observer);
                        observer.propertyChanged(null, content);
                    }
                }

                this.updatePage(page);
            });

            this._addedPages.forEach((p)=> {
                if (pagesInViewport.indexOf(p) == -1) {
                    this.removePage(p);
                }
            });
        }



        private updatePage(page:model.AbstractPage) { 
            var pai = this._area.getPageInformation(page);
            var size = page.size.scale(pai.scale);
            var halfSize = size.scale(0.5);
            var pageRect = new Rect(new Position2D(pai.position.x - halfSize.width, pai.position.y - halfSize.height), size);
            var vpRect = this._vp.asRectInArea();
            var dpr = (window.devicePixelRatio||1);

            var pagePos = new Position2D(pageRect.pos.x - vpRect.pos.x, pageRect.pos.y - vpRect.pos.y);
            var scaledPagePos = pagePos.scale(this._vp.scale / dpr);
            var pe = this._pageElementCache.get(page);

            var realSize = size.scale(1 / pai.scale);

            pe.style.cssText =
                `
                position: absolute;
                transform-origin : 0% 0%;
                width : ${realSize.width}px;
                height : ${realSize.height}px;
                transform : translate(${Math.round(scaledPagePos.x)}px,${Math.round(scaledPagePos.y)}px) scale(${pai.scale * this._vp.scale / dpr});
                `;


            var childrenElement = <HTMLElement>pe.children[0];
            childrenElement.style.cssText = "transform : rotate(" + pai.rotation + "deg);" +
                "width: " + realSize.width + "px;" +
                "height: " + realSize.height + "px;"+
                "background-color : transparent;";

        }

        private addPage(page:model.AbstractPage) {
            if (!this._pageElementCache.has(page)) {
                this.createPageElement(page);
            }

            var pageElement = this._pageElementCache.get(page);
            this._addedPages.push(page);
            this.htmlContainer.appendChild(pageElement);
        }

        private createPageElement(page:model.AbstractPage) {
            var pageElement = document.createElement("div");
            var childPageElement = <HTMLDivElement>(pageElement.cloneNode());
            pageElement.appendChild(childPageElement);
            this._pageElementCache.set(page, pageElement);
        }

        private removePage(page:model.AbstractPage) {
            this._addedPages.splice(this._addedPages.indexOf(page), 1);
            var textContent = this._pageElementCache.get(page);
            textContent.parentElement.removeChild(textContent);
        }

    }
}
