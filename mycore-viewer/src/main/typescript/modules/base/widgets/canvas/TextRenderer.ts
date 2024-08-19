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

import {Viewport} from "./viewport/Viewport";
import {PageArea} from "./model/PageArea";
import {PageView} from "./PageView";
import {AbstractPage} from "../../components/model/AbstractPage";
import {MyCoReMap, Position2D, Rect} from "../../Utils";
import {TextContentModel, TextElement} from "../../components/model/TextContent";

export class TextRenderer {
    constructor(private _vp: Viewport, private _area: PageArea, private _view: PageView, private _textContentProvider: (page: AbstractPage, contentProvider: (textContent: TextContentModel) => void) => void, private pageLinkClicked: (page: string) => void) {
        this.textContainer = document.createElement("div");
        this.textContainer.style.cssText = "line-height: 1;" +
            "white-space: pre;" +
            "font-family: sans-serif";


        this.textContainer.setAttribute("class", "textContainer");
        const htmlElement = this._view.container[0] as HTMLElement;
        htmlElement.appendChild(this.textContainer);
    }

    private _contentCache: MyCoReMap<AbstractPage, TextContentModel> = new MyCoReMap<AbstractPage, TextContentModel>();
    private _callbackRunning: MyCoReMap<AbstractPage, boolean> = new MyCoReMap<AbstractPage, boolean>();
    private _addedPages: Array<AbstractPage> = new Array<AbstractPage>();
    public textContainer: HTMLElement;
    private _elementCache: MyCoReMap<string, HTMLElement> = new MyCoReMap<string, HTMLElement>();
    private _pageElementCache: MyCoReMap<AbstractPage, HTMLElement> = new MyCoReMap<AbstractPage, HTMLElement>();
    private _lineElementMap: MyCoReMap<TextElement, HTMLElement> = new MyCoReMap<TextElement, HTMLElement>()
    private _highlightWordMap: MyCoReMap<TextElement, Array<string>> = null;
    private _idPageMap = new MyCoReMap<string, AbstractPage>();
    private _mesureCanvas = <CanvasRenderingContext2D>document.createElement("canvas").getContext("2d");

    public update() {
        const pagesInViewport = this._area.getPagesInViewport(this._vp);
        pagesInViewport.forEach((page: AbstractPage) => {
            if (!this._idPageMap.has(page.id)) {
                this._idPageMap.set(page.id, page);
            }

            if (this._addedPages.indexOf(page) == -1) {
                this.addPage(page);
            }

            if (!this._contentCache.has(page) && !this._callbackRunning.has(page)) {
                const promise = this._textContentProvider(page, (content) => {
                    this._contentCache.set(page, content);
                    this.addPageParts(page, content);
                });
                this._callbackRunning.set(page, true);
            } else if (this._contentCache.has(page)) {
                this.updatePage(page);
            }
        });

        this._addedPages.forEach((p) => {
            if (pagesInViewport.indexOf(p) == -1) {
                this.removePage(p);
            }
        });
    }

    private updatePage(page: AbstractPage) {
        const pai = this._area.getPageInformation(page);
        const size = page.size.scale(pai.scale);
        const halfSize = size.scale(0.5);
        const pageRect = new Rect(new Position2D(pai.position.x - halfSize.width, pai.position.y - halfSize.height), size);
        const vpRect = this._vp.asRectInArea();
        const dpr = (window.devicePixelRatio || 1);

        const pagePos = new Position2D(pageRect.pos.x - vpRect.pos.x, pageRect.pos.y - vpRect.pos.y);
        const scaledPagePos = pagePos.scale(this._vp.scale / dpr);
        const pe = this._pageElementCache.get(page);

        const realSize = size.scale(1 / pai.scale);

        pe.style.cssText =
            "transform-origin : 0% 0%;" +
            "position : absolute;" +
            "left : " + scaledPagePos.x + "px;" +
            "top : " + scaledPagePos.y + "px;" +
            "width : " + realSize.width + "px;" +
            "height : " + realSize.height + "px;" +
            "transform : " + "scale(" + (pai.scale * this._vp.scale / dpr) + ");" +
            "z-index: 5;";

        const childrenElement = pe.children[0] as HTMLElement;
        childrenElement.style.cssText = "transform : rotate(" + pai.rotation + "deg);" +
            "width: " + realSize.width + "px;" +
            "height: " + realSize.height + "px;"


    }

    private addPage(page: AbstractPage) {
        if (!this._pageElementCache.has(page)) {
            this.createPageElement(page);
        }

        const pageElement = this._pageElementCache.get(page);
        this._addedPages.push(page);
        this.textContainer.appendChild(pageElement);
    }

    createPageElement(page) {
        const pageElement = document.createElement("div");
        const childPageElement = <HTMLDivElement>(pageElement.cloneNode());
        pageElement.appendChild(childPageElement);
        this._pageElementCache.set(page, pageElement);
        pageElement.style.display = "none";
    }

    private removePage(page: AbstractPage) {
        this._addedPages.splice(this._addedPages.indexOf(page), 1);
        const textContent = this._pageElementCache.get(page);
        textContent.parentElement.removeChild(textContent);
    }

    private addPageParts(page: AbstractPage, textContent: TextContentModel) {
        const pageHtml = document.createDocumentFragment();
        textContent.content.forEach((e) => {
            const cacheKey = e.pos.toString() + e.text;
            if (!this._elementCache.has(cacheKey)) {
                const contentPart = this.createContentPart(page, e);
                pageHtml.appendChild(contentPart);
            }
        });
        textContent.links.forEach((link) => {
            const cacheKey = link.rect.toString() + link.url;
            if (!this._elementCache.has(cacheKey)) {
                const linkElement = document.createElement("a");
                linkElement.setAttribute("href", link.url);
                linkElement.style.left = link.rect.getX() + "px";
                linkElement.style.top = link.rect.getY() + "px";
                linkElement.style.width = link.rect.getWidth() + "px";
                linkElement.style.height = link.rect.getHeight() + "px";
                linkElement.style.display = "block";
                linkElement.style.position = "fixed";
                linkElement.style.zIndex = "8";
                linkElement.setAttribute("target", "_blank");
                this._elementCache.set(cacheKey, linkElement);
                pageHtml.appendChild(linkElement);
            }
        });

        textContent.internLinks.forEach((link) => {
            const cacheKey = link.rect.toString() + "DEST";
            if (!this._elementCache.has(cacheKey)) {
                const linkElement = document.createElement("a");
                linkElement.style.left = link.rect.getX() + "px";
                linkElement.style.top = link.rect.getY() + "px";
                linkElement.style.width = link.rect.getWidth() + "px";
                linkElement.style.height = link.rect.getHeight() + "px";
                linkElement.style.display = "block";
                linkElement.style.position = "fixed";
                linkElement.style.zIndex = "8";
                linkElement.style.cursor = 'pointer';
                linkElement.addEventListener('click', () => {
                    link.pageNumberResolver((number) => {
                        this.pageLinkClicked(number);
                    });
                });
                this._elementCache.set(cacheKey, linkElement);
                pageHtml.appendChild(linkElement);
            }

        });

        const pageElement = <HTMLElement>this._pageElementCache.get(page).children[0];
        pageElement.appendChild(pageHtml);
        if (pageElement.style.display == "none") {
            pageElement.style.display = "block";
        }
    }

    private removeContentPart(cp: TextElement) {
        const cpElement = this._lineElementMap.get(cp);
        const parent = cpElement.parentElement;

        parent.removeChild(cpElement);
        this._lineElementMap.remove(cp);
    }

    private createContentPart(page: AbstractPage, cp: TextElement): HTMLElement {
        const htmlElement = window.document.createElement("div");
        htmlElement.textContent = cp.text;
        htmlElement.setAttribute("class", "line");

        // need to stop propagation, because the text should be selectable and one parent calls preventDefault
        // (which prevents the selection)
        const stopPropagation = (e) => {
            e.stopPropagation();
        };

        htmlElement.addEventListener("mousedown", stopPropagation);
        htmlElement.addEventListener("mouseup", stopPropagation);
        htmlElement.addEventListener("mousemove", stopPropagation);

        htmlElement.addEventListener("mouseenter", () => {
            if ("mouseenter" in cp && typeof cp.mouseenter == "function") {
                cp.mouseenter();
            }
        });

        htmlElement.addEventListener("mouseleave", () => {
            if ("mouseleave" in cp && typeof cp.mouseleave == "function") {
                cp.mouseleave();
            }
        });

        const size = page.size;
        const cacheKey = cp.pos.toString() + cp.text;

        this._elementCache.set(cacheKey, htmlElement);

        this._mesureCanvas.save();
        this._mesureCanvas.font = cp.fontSize + 'px ' + cp.fontFamily;

        const drawnWidth = this._mesureCanvas.measureText(cp.text).width;
        this._mesureCanvas.restore();

        const xScaling = cp.size.width / drawnWidth;
        this._lineElementMap.set(cp, htmlElement);

        const topPosition = ("fromBottomLeft" in cp && !cp.fromBottomLeft) ? cp.pos.y : size.height - cp.pos.y;

        htmlElement.style.cssText = "left : " + cp.pos.x + "px;" +
            "top : " + topPosition + "px;" +
            "font-family : " + cp.fontFamily + ";" +
            "font-size : " + cp.fontSize + "px;" +
            "transform-origin : 0% 0%;" +
            "transform : scalex(" + xScaling + ");";

        return htmlElement;
    }

}

