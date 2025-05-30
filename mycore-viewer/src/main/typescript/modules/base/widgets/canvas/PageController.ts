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
import {getElementHeight, getElementWidth, MyCoReMap, Position2D, Rect, Size2D, ViewerProperty} from "../../Utils";
import { PageArea, PageAreaInformation } from "./model/PageArea";
import { HtmlRenderer } from "./HtmlRenderer";
import { PageView } from "./PageView";
import { TextRenderer } from "./TextRenderer";
import { CanvasPageLayer } from "./CanvasPageLayer";
import { Overview } from "./Overview";
import { AbstractPage } from "../../components/model/AbstractPage";
import { Animation } from "./Animation";


export class PageController {

  constructor(private miniOverview: boolean = false) {
    this._pageArea.updateCallback = () => {
      this.update();
    };

    this._updateSizeIfChanged();
    this._registerViewport();
  }

  private _lastSize: Size2D = new Size2D(0, 0);
  private _requestRunning: boolean = false;
  private _nextRequested: boolean = false;
  private _pageArea: PageArea = new PageArea();
  private _viewport: Viewport = new Viewport();
  private _views: Array<PageView> = new Array<PageView>();
  private _viewHTMLRendererMap = new MyCoReMap<PageView, HtmlRenderer>();
  private _textRenderer: TextRenderer = null;
  private _canvasPageLayers = new MyCoReMap<number, CanvasPageLayer>();
  private _lastAnimationTime: number = null;
  private _animations = new Array<Animation>();

  public _overview: Overview = null;

  public _updateSizeIfChanged() {
    this._views.forEach(view => {
      const retinaWidth = Math.round(getElementWidth(view.container) * (window.devicePixelRatio || 1));
      const retinaHeight = Math.round(getElementHeight(view.container) * (window.devicePixelRatio || 1));
      if (view.drawCanvas.width != retinaWidth || view.drawCanvas.height != retinaHeight || view.markCanvas.width != retinaWidth || view.markCanvas.height != retinaHeight) {
        this._updateSize(view);
      }
    });

  }

  private _updateSize(view: PageView) {
    view.drawCanvas.width = getElementWidth(view.container) * (window.devicePixelRatio || 1);
    view.drawCanvas.height = getElementHeight(view.container) * (window.devicePixelRatio || 1);
    view.markCanvas.width = getElementWidth(view.container) * (window.devicePixelRatio || 1);
    view.markCanvas.height = getElementHeight(view.container) * (window.devicePixelRatio || 1);
    this._lastSize = new Size2D(view.drawCanvas.width, view.drawCanvas.height);
    this._viewport.size = new Size2D(view.drawCanvas.width, view.drawCanvas.height);
  }

  private _unregisterViewport() {
    if (this._viewport != null) {
      this._viewport.sizeProperty.removeAllObserver();
      this._viewport.positionProperty.removeAllObserver();
      this._viewport.scaleProperty.removeAllObserver();
      this._viewport.rotationProperty.removeAllObserver();
    }
  }

  private _registerViewport() {
    const updater = {
      propertyChanged: (_old: ViewerProperty<any>, _new: ViewerProperty<any>) => {
        this.update();
      }
    };

    this._viewport.sizeProperty.addObserver(updater);
    this._viewport.positionProperty.addObserver(updater);
    this._viewport.scaleProperty.addObserver(updater);
    this._viewport.rotationProperty.addObserver(updater);
  }

  public update(): void {
    if (!this._nextRequested) {
      this._nextRequested = true;
      if (!this._requestRunning) {
        this._requestRunning = true;
        requestAnimationFrame(() => {
          this._nextRequested = false;
          this._updateSizeIfChanged();
          this.updateAnimations();
          this._views.forEach(view => {
            if (view.drawHTML) {
              let htmlRenderer: HtmlRenderer;
              if (this._viewHTMLRendererMap.has(view)) {
                htmlRenderer = this._viewHTMLRendererMap.get(view);
              } else {
                htmlRenderer = new HtmlRenderer(this._viewport, this._pageArea, view);
                this._viewHTMLRendererMap.set(view, htmlRenderer);
              }
              htmlRenderer.update();
            }
            this.drawOnView(view, this.viewport, !view.drawImage);
          });

          if (this._textRenderer != null) {
            this._textRenderer.update();
          }

          if (this._overview != null) {
            this.drawOnView(this._overview, this._overview.overviewViewport);
            this._overview.drawRect();
          }

          //this._ocrRenderer.update();
        });
        this._requestRunning = false;
      }
    }
  }


  private drawOnView(view: PageView, vp: Viewport = this._viewport, markerOnly: boolean = false): void {
    if (view != null && vp != null) {
      view.drawCanvas.width = view.drawCanvas.width;
      view.markCanvas.width = view.markCanvas.width;

      const rotatedViewportSize = vp.size.getRotated(vp.rotation);
      const ctx1 = <CanvasRenderingContext2D>view.drawCanvas.getContext("2d");
      const ctx2 = <CanvasRenderingContext2D>view.markCanvas.getContext("2d");

      ctx1.save();
      ctx2.save();
      {
        {
          ctx1.translate(vp.size.width / 2, vp.size.height / 2);
          ctx1.rotate(vp.rotation * Math.PI / 180);
          ctx1.translate(-rotatedViewportSize.width / 2, -rotatedViewportSize.height / 2);
        }
        {
          ctx2.translate(vp.size.width / 2, vp.size.height / 2);
          ctx2.rotate(vp.rotation * Math.PI / 180);
          ctx2.translate(-rotatedViewportSize.width / 2, -rotatedViewportSize.height / 2);
        }
        this._pageArea.getPagesInViewport(vp).forEach((page: AbstractPage) => {
          this.drawPage(page, this._pageArea.getPageInformation(page), vp.asRectInArea(), vp.scale, vp != this._viewport, view, markerOnly);
        });
      }
      ctx1.restore();
      ctx2.restore();
    }
  }

  public drawPage(page: AbstractPage, info: PageAreaInformation, areaInViewport: Rect, scale: number, preview: boolean, view: PageView, markerOnly: boolean = false) {
    const realPageDimension = page.size.getRotated(info.rotation).scale(info.scale);
    const pageRect = new Rect(new Position2D(info.position.x - (realPageDimension.width / 2), info.position.y - (realPageDimension.height / 2)), realPageDimension);

    // This is the area of the page wich will be drawed in absolute coordinates
    const pagePartInArea = areaInViewport.getIntersection(pageRect);

    /*
     * This is the area of the page wich will be drawed in relative coordinates(to the page)
     * If the page is not rotatet its okay, but if the page is rotatet 90deg we need to rotate it back
     */
    const pagePartInPageRotated = new Rect(new Position2D(Math.max(0, pagePartInArea.pos.x - pageRect.pos.x), Math.max(0, pagePartInArea.pos.y - pageRect.pos.y)), pagePartInArea.size);

    const rotateBack = (deg: number, pagePartBefore: Rect, realPageDimension: Size2D) => {
      if (deg == 0) {
        return pagePartBefore;
      }
      const newPositionX = pagePartBefore.pos.y;
      // we use realPageDimension.width instead of height because realPageDimension is the rotated value
      const newPositionY = realPageDimension.width - pagePartBefore.pos.x - pagePartBefore.size.width;
      const rect = new Rect(new Position2D(newPositionX, newPositionY), pagePartBefore.size.getRotated(90));
      if (deg == 90) {
        return rect;
      } else {
        return rotateBack(deg - 90, rect, realPageDimension.getRotated(90));
      }
    };

    const pagePartInPage = rotateBack(info.rotation, pagePartInPageRotated, realPageDimension);
    const notRotated = page.size.scale(info.scale);
    const ctx1 = <CanvasRenderingContext2D>view.drawCanvas.getContext("2d");
    const ctx2 = <CanvasRenderingContext2D>view.markCanvas.getContext("2d");
    ctx1.save();
    ctx2.save();
    {
      {
        ctx1.translate(Math.floor((-areaInViewport.pos.x + info.position.x) * scale), Math.floor((-areaInViewport.pos.y + info.position.y) * scale));
        ctx1.rotate(info.rotation * Math.PI / 180);
        ctx1.translate(Math.floor(((-notRotated.width / 2) + pagePartInPage.pos.x) * scale), Math.floor(((-notRotated.height / 2) + pagePartInPage.pos.y) * scale));

        ctx2.translate(Math.floor((-areaInViewport.pos.x + info.position.x) * scale), Math.floor(-areaInViewport.pos.y + info.position.y) * scale);
        ctx2.rotate(info.rotation * Math.PI / 180);
        ctx2.translate((Math.floor(-notRotated.width / 2) + pagePartInPage.pos.x) * scale, Math.floor((-notRotated.height / 2) + pagePartInPage.pos.y) * scale);
      }

      let realAreaToDraw = pagePartInPage.scale(1 / info.scale);
      realAreaToDraw = new Rect(realAreaToDraw.pos.roundDown(), realAreaToDraw.size.roundDown());

      if (!markerOnly) {
        page.draw(ctx1, realAreaToDraw, scale * info.scale, preview);
      }

      ctx2.translate(-realAreaToDraw.pos.x * scale * info.scale, -realAreaToDraw.pos.y * scale * info.scale);
      if (!preview) {
        ctx2.scale(scale * info.scale, scale * info.scale);
        const layers: Array<CanvasPageLayer> = this.getCanvasPageLayersOrdered();
        layers.forEach(layer => {
          layer.draw(ctx2, page.id, page.size, view.drawHTML);
        });
        ctx2.scale(1 / scale * info.scale, 1 / scale * info.scale);
      }
    }
    ctx1.restore();
    ctx2.restore();
  }

  private updateAnimations() {
    this._viewport.updateAnimation();
    if (this._animations.length == 0) {
      this._lastAnimationTime = null;
      return;
    }
    if (this._lastAnimationTime == null) {
      this._lastAnimationTime = new Date().valueOf();
    }
    const elapsedTime: number = new Date().valueOf() - this._lastAnimationTime;
    const finishedAnimations: Array<Animation> = [];
    for (let animation of this._animations) {
      if (animation.updateAnimation(elapsedTime)) {
        finishedAnimations.push(animation);
      }
    }
    finishedAnimations.forEach(animation => {
      this.removeAnimation(animation);
    });
    if (this._animations.length == 0) {
      this._lastAnimationTime = null;
      return;
    }
    setTimeout(() => {
      this.update();
    }, 0);
  }

  public addAnimation(animation: Animation) {
    this._animations.push(animation);
  }

  public removeAnimation(animation: Animation) {
    const index = this._animations.indexOf(animation);
    if (index >= 0) {
      this._animations.splice(index, 1);
    }
  }

  public get viewport(): Viewport {
    return this._viewport;
  }


  public get views(): Array<PageView> {
    return this._views;
  }

  public addPage(page: AbstractPage, info: PageAreaInformation): void {
    page.refreshCallback = () => {
      this.update();
    };
    this._pageArea.addPage(page, info);
  }

  public removePage(page: AbstractPage): void {
    page.refreshCallback = () => {
    };
    page.clear();
    this._pageArea.removePage(page);
  }


  public getPages(): Array<AbstractPage> {
    return this._pageArea.getPages();
  }

  public setPageAreaInformation(page: AbstractPage, info: PageAreaInformation): void {
    this._pageArea.setPageAreaInformation(page, info);
  }

  public getPageAreaInformation(page: AbstractPage) {
    return this._pageArea.getPageInformation(page);
  }

  public addCanvasPageLayer(zIndex: number, canvas: CanvasPageLayer) {
    this._canvasPageLayers.set(zIndex, canvas);
  }

  public getCanvasPageLayers(): MyCoReMap<number, CanvasPageLayer> {
    return this._canvasPageLayers;
  }

  public getCanvasPageLayersOrdered(): Array<CanvasPageLayer> {
    if (this._canvasPageLayers == null || this._canvasPageLayers.isEmpty()) {
      return [];
    }
    const sortedArray: Array<CanvasPageLayer> = [];
    this._canvasPageLayers.keys.sort().forEach(k => {
      sortedArray.push(this._canvasPageLayers.get(k));
    });
    return sortedArray;
  }

  public getPageArea(): PageArea {
    return this._pageArea;
  }


  get textRenderer(): TextRenderer {
    return this._textRenderer;
  }

  set textRenderer(value: TextRenderer) {
    this._textRenderer = value;
  }
}



