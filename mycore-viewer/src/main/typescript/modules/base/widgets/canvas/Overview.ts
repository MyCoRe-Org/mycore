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

import { PageView } from "./PageView";
import { Viewport } from "./viewport/Viewport";
import {offset, Position2D, Rect, Size2D} from "../../Utils";

export class Overview extends PageView {

  constructor(private vp: Viewport, private _maxOverviewSize: Size2D = new Size2D(250, 250)) {
    super(true, false);
    this.container.classList.add("overview");
    this.container.setAttribute("style", "");
    this.container.style.zIndex = "6";
    this.markCanvas.remove()
    //this.updateOverviewSize(_maxOverviewSize);
  }

  private updateOverviewSize(size: Size2D) {
    size = size.roundUp();
    this.container.style.width = size.width + "px";
    this.container.style.height = size.height + "px";
    if (this.drawCanvas.width != size.width || this.drawCanvas.height != size.height
      || this.markCanvas.width != size.width
      || this.markCanvas.height != size.height) {
      this.drawCanvas.width = size.width;
      this.drawCanvas.height = size.height;
      this.markCanvas.width = size.width;
      this.markCanvas.height = size.height;
    }
    this.overviewViewport.size = size;
  }

  public overviewViewport: Viewport = new Viewport();
  private _overviewRect: Rect;

  public set overviewRect(rect: Rect) {
    this.overviewViewport.position = rect.getMiddlePoint();
    const scale = this.overviewViewport.scale = Math.min(this._maxOverviewSize.width / rect.size.width,
      this._maxOverviewSize.height / rect.size.height);
    const toWidth = this._maxOverviewSize.width / rect.size.width == scale;
    let realSize;
    if (toWidth) {
      const relation = rect.size.width / rect.size.height;
      realSize = new Size2D(this._maxOverviewSize.width, this._maxOverviewSize.height / relation);
    } else {
      const relation = rect.size.height / rect.size.width;
      realSize = new Size2D(this._maxOverviewSize.width / relation, this._maxOverviewSize.height);
    }
    this.updateOverviewSize(realSize);

    this._overviewRect = rect;
  }


  public get overviewRect() {
    return this.overviewViewport.asRectInArea();
  }

  public drawRect() {
    const ctx = <CanvasRenderingContext2D>this.drawCanvas.getContext("2d");
    const overviewArea = this.overviewViewport.asRectInArea();
    const vpArea = this.vp.asRectInArea();
    const lineWidth = 500;

    let pos = new Position2D(vpArea.pos.x - overviewArea.pos.x, vpArea.pos.y - overviewArea.pos.y);
    pos = pos.scale(this.overviewViewport.scale);

    let vpSizeInOverview = this.vp.asRectInArea().size.scale(this.overviewViewport.scale);
    ctx.save();
    {
      ctx.lineWidth = lineWidth;
      ctx.strokeStyle = "rgba(0,0,0,0.5)";
      ctx.translate(-lineWidth / 2, -lineWidth / 2);
      ctx.strokeRect(
        pos.x,
        pos.y,
        vpSizeInOverview.width + (lineWidth),
        vpSizeInOverview.height + (lineWidth)
      )
    }
    ctx.restore();
  }

  public initEventHandler() {
    const handler = (e: MouseEvent) => {
      e.preventDefault();
      if(!("classList" in e.target)) {
        return;
      }
      const target = e.target as HTMLElement;
      const x = (e.clientX + window.pageXOffset) - offset(target).left;
      const y = (e.clientY + window.pageYOffset) - offset(target).top;
      const pos = new Position2D(x, y);
      const scaledPos = pos.scale(1 / this.overviewViewport.scale);
      const upperLeftVpPos = this.overviewViewport.asRectInArea().getPoints().upperLeft;
      const correctedPos = new Position2D(scaledPos.x + upperLeftVpPos.x, scaledPos.y + upperLeftVpPos.y);

      this.vp.position = correctedPos;
      e.stopImmediatePropagation();
    };

    this.drawCanvas.addEventListener('mousedown', (e) => {
      this.drawCanvas.addEventListener("mousemove", handler);
    });
    this.drawCanvas.addEventListener('mouseup', (e) => {
      this.drawCanvas.removeEventListener("mousemove", handler);
    });
    this.drawCanvas.addEventListener('mouseout' ,(e) => {
      this.drawCanvas.removeEventListener("mousemove", handler);
    });

  }

}



