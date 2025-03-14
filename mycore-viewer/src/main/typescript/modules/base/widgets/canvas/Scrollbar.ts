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

import { Size2D } from "../../Utils";

export class Scrollbar {

  constructor(private _horizontal: boolean) {
    this.initElements();
    let body = jQuery(document.body);

    let moveHandler = (e) => {
      if (this._mouseDown != -1) {
        const val = (this._horizontal ? (e.clientX - this._scrollbarElement.offset().left) : (e.clientY - this._scrollbarElement.offset().top)) - this._mouseDown;
        const realSize = (this._horizontal ? this._scrollbarElement.width() : this._scrollbarElement.height()) - 30;
        const relation = realSize / this._areaSize;
        this._position = (val) / relation;
        this.update();
        if (this.scrollHandler != null) {
          this.scrollHandler();
        }
        e.preventDefault();
      }
    };

    let upHandler = (e) => {
      this._mouseDown = -1;
      if (interv != -1) {
        window.clearInterval(interv);
        interv = -1;
        e.preventDefault();
      }
      body.unbind("mousemove", moveHandler);
    };

    this._slider.mousedown((e) => {
      this._mouseDown = this._horizontal ? (e.clientX - this._slider.offset().left) : (e.clientY - this._slider.offset().top);
      body.bind("mousemove", moveHandler);
      body.bind("mouseup", upHandler);
      e.preventDefault();
    });

    this._scrollbarElement.mousedown((e) => {
      if (jQuery(e.target).hasClass("slider")) {
        return;
      }
      let val = (this._horizontal ? (e.clientX - this._scrollbarElement.offset().left) : (e.clientY - this._scrollbarElement.offset().top));
      let realSize = (this._horizontal ? this._scrollbarElement.width() : this._scrollbarElement.height()) - 30;
      let relation = realSize / this._areaSize;
      let sliderSize = Math.min(Math.max(20, this._viewSize * relation), realSize);
      this._position = (val - (sliderSize / 2)) / relation;
      this.update();
      if (this.scrollHandler != null) {
        this.scrollHandler();
      }
    });

    let interv = -1;
    this._startButton.mousedown((e) => {
      this._position -= 200;
      this.scrollHandler();
      e.preventDefault();
      e.stopImmediatePropagation();
      interv = window.setInterval(() => {
        this._position -= 200;
        this.scrollHandler();
      }, 111);
    });

    this._endButton.mousedown((e) => {
      this._position += 200;
      this.scrollHandler();
      e.preventDefault();
      e.stopImmediatePropagation();
      interv = window.setInterval(() => {
        this._position += 200;
        this.scrollHandler();
      }, 111);
    });

    jQuery(document.body).mousemove((e) => {
      if (this._mouseDown != -1) {
        let val = (this._horizontal ? (e.clientX - this._scrollbarElement.offset().left) :
          (e.clientY - this._scrollbarElement.offset().top)) - this._mouseDown;
        let realSize = (this._horizontal ? this._scrollbarElement.width() :
          this._scrollbarElement.height()) - 30;
        let relation = realSize / this._areaSize;
        this._position = (val) / relation;
        this.update();
        if (this.scrollHandler != null) {
          this.scrollHandler();
        }
      }
    });

    jQuery(document.body).mouseup((e) => {
      this._mouseDown = -1;
      if (interv != -1) {
        window.clearInterval(interv);
        interv = -1;
      }
    });

  }

  public clearRunning() {
    this._mouseDown = -1;
  }

  private initElements() {
    this._className = (this._horizontal ? "horizontal" : "vertical");

    this._scrollbarElement = jQuery("<div></div>");
    this._scrollbarElement.addClass(this._className + "Bar");

    this._slider = jQuery("<div></div>");
    this._slider.addClass("slider");

    this._startButton = jQuery("<div></div>");
    this._startButton.addClass("startButton");

    this._endButton = jQuery("<div></div>");
    this._endButton.addClass("endButton");

    this._startButton.appendTo(this._scrollbarElement);
    this._slider.appendTo(this._scrollbarElement);
    this._endButton.appendTo(this._scrollbarElement);
  }

  private _scrollbarElement: JQuery;
  private _slider: JQuery;
  private _areaSize: number = null;
  private _viewSize: number = null;
  private _position: number = null;
  private _className: string;
  private _startButton: JQuery;
  private _endButton: JQuery;
  private _mouseDown: number = -1;
  private _scrollhandler: () => void = null;

  public get viewSize() {
    return this._viewSize;
  }

  public get areaSize() {
    return this._areaSize;
  }

  public set viewSize(view: number) {
    this._viewSize = view;
    this.update();
  }

  public set areaSize(area: number) {
    this._areaSize = area;
    this.update();
  }

  public get position() {
    return this._position;
  }

  public set position(pos: number) {
    this._position = pos;
    this.update();
  }

  public update() {
    if (this._areaSize == null || this._viewSize == null || this._position == null) {
      return;
    }
    const ret = this.getScrollbarElementSize();
    const realSize = (this._horizontal ? ret.width : ret.height) - 30;
    let relation = realSize / this._areaSize;

    // calculate and set slider style
    const sliderSize = Math.min(Math.max(20, this._viewSize * relation), realSize);
    const sliderSizeStyleKey = this._horizontal ? "width" : "height";
    const sliderSizeStyle = {};
    sliderSizeStyle[sliderSizeStyleKey] = sliderSize + "px";
    this._slider.css(sliderSizeStyle);

    relation = (realSize - (sliderSize - (this._viewSize * relation))) / this._areaSize;

    //calculate and set slider position
    const sliderPos = Math.min(Math.max(this._position * relation + 15, 15), (this.areaSize * relation) - sliderSize) + 15;
    const sliderPosStyleKey = this._horizontal ? "left" : "top";
    const sliderPosStyle = {};
    sliderPosStyle[sliderPosStyleKey] = sliderPos + "px";
    this._slider.css(sliderPosStyle);


  }

  private _cachedScrollbarElementSize: Size2D = null;
  private _cacheTime: number = -1;

  private getScrollbarElementSize() {
    const currentTime = new Date().getTime();
    if (this._cachedScrollbarElementSize == null || (currentTime - 1000 > this._cacheTime)) {
      const elementHeight = this._scrollbarElement.height();
      const elementWidth = this._scrollbarElement.width();
      this._cachedScrollbarElementSize = new Size2D(elementWidth, elementHeight);
      this._cacheTime = new Date().getTime();
    }

    return this._cachedScrollbarElementSize;
  }

  public resized() {
    this._cachedScrollbarElementSize = null;
  }

  public get scrollbarElement() {
    return this._scrollbarElement;
  }

  public get scrollHandler() {
    return this._scrollhandler;
  }

  public set scrollHandler(handler: () => void) {
    this._scrollhandler = handler;
  }
}

