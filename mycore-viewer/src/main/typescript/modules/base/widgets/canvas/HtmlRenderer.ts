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

import { Viewport } from "./viewport/Viewport";
import { PageArea } from "./model/PageArea";
import { PageView } from "./PageView";
import { MyCoReMap, Position2D, Rect, ViewerProperty } from "../../Utils";
import { AbstractPage } from "../../components/model/AbstractPage";

export class HtmlRenderer {
  constructor(private _vp: Viewport, private _area: PageArea, private _view: PageView) {
    this.htmlContainer = document.createElement("div");
    this.htmlContainer.setAttribute("class", "textContainer");
    const htmlElement = <HTMLElement>this._view.container[0];
    htmlElement.appendChild(this.htmlContainer);
  }

  private _addedPages: Array<AbstractPage> = new Array<AbstractPage>();
  public htmlContainer: HTMLElement;
  private _pageElementCache: MyCoReMap<AbstractPage, HTMLElement> = new MyCoReMap<AbstractPage, HTMLElement>();
  private _idPageMap = new MyCoReMap<string, AbstractPage>();
  private _addedContentMap = new MyCoReMap<AbstractPage, ViewerProperty<HTMLElement>>();

  public update() {
    const pagesInViewport = this._area.getPagesInViewport(this._vp);
    pagesInViewport.forEach((page: AbstractPage) => {
      if (!this._idPageMap.has(page.id)) {
        this._idPageMap.set(page.id, page);
      }

      if (this._addedPages.indexOf(page) == -1) {
        this.addPage(page);
      }

      if (!this._addedContentMap.has(page)) {
        if ("getHTMLContent" in page) {
          let content = (<any>page).getHTMLContent();
          this._addedContentMap.set(page, content);
          let observer = {
            propertyChanged: (_old: ViewerProperty<HTMLElement>, _new: ViewerProperty<HTMLElement>) => {
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
      `
                position: absolute;
                transform-origin : 0% 0%;
                width : ${realSize.width}px;
                height : ${realSize.height}px;
                transform : translate(${Math.round(scaledPagePos.x)}px,${Math.round(scaledPagePos.y)}px) scale(${pai.scale * this._vp.scale / dpr});
                `;


    const childrenElement = <HTMLElement>pe.children[0];
    childrenElement.style.cssText = "transform : rotate(" + pai.rotation + "deg);" +
      "width: " + realSize.width + "px;" +
      "height: " + realSize.height + "px;" +
      "background-color : transparent;";

  }

  private addPage(page: AbstractPage) {
    if (!this._pageElementCache.has(page)) {
      this.createPageElement(page);
    }

    const pageElement = this._pageElementCache.get(page);
    this._addedPages.push(page);
    this.htmlContainer.appendChild(pageElement);
  }

  private createPageElement(page: AbstractPage) {
    const pageElement = document.createElement("div");
    const childPageElement = <HTMLDivElement>(pageElement.cloneNode());
    pageElement.appendChild(childPageElement);
    this._pageElementCache.set(page, pageElement);
  }

  private removePage(page: AbstractPage) {
    this._addedPages.splice(this._addedPages.indexOf(page), 1);
    const textContent = this._pageElementCache.get(page);
    textContent.parentElement.removeChild(textContent);
  }

}
