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


import { Viewport } from "../viewport/Viewport";
import {offset, Position2D, Rect, viewerCrossBrowserWheel} from "../../../Utils";
import { DesktopInputListener } from "./DesktopInputListener";

export class DesktopInputDelegator {
  constructor(private _inputElement: HTMLElement, private _viewport: Viewport, private _handler: DesktopInputListener) {
    this.initMove();
    this.initScale();
  }

  private _overviewRect: Rect;
  private _overviewBounds: Rect;
  private _overviewScale: number;

  private _lastMouseSession: MouseSession = null;
  private _currentMouseSession: MouseSession;

  private _mouseDownHandler: (e: MouseEvent) => void;
  private _mouseUpHandler: (e: MouseEvent) => void;
  private _mouseMoveHandler: (e: MouseEvent) => void;
  private _mouseDragHandler: (e: MouseEvent) => void;
  private _mouseLeaveHandler: (e: MouseEvent) => void;

  public initMove(): void {
    let inputElement = this._inputElement;

    this._mouseMoveHandler = (e: MouseEvent) => {
      let target = this.getTarget(e.target);
      if (target == null) {
        return;
      }
      let mousePosition: Position2D = this.getMousePosition(inputElement, e);
      this._handler.mouseMove(mousePosition, e);
    };

    this._mouseDragHandler = (e: MouseEvent) => {
      let target = this.getTarget(e.target);
      if (target == null) {
        return;
      }
      let mousePosition: Position2D = this.getMousePosition(inputElement, e);
      this._handler.mouseDrag(mousePosition,
        this._currentMouseSession.startPosition,
        this._currentMouseSession.startViewport, e);
    };

    this._mouseDownHandler = (e: MouseEvent) => {
      let target = this.getTarget(e.target);
      if (target == null) {
        return;
      }
      let mousePosition: Position2D = this.getMousePosition(inputElement, e);
      this._handler.mouseDown(mousePosition, e);

      // start mouse session for drag and double click support
      this._currentMouseSession = this.createMouseSession(mousePosition, this._viewport.position.copy());
      inputElement.removeEventListener("mousemove", this._mouseMoveHandler);
      inputElement.addEventListener("mousemove", this._mouseDragHandler);
      inputElement.addEventListener("mouseleave", this._mouseLeaveHandler);

    };

    this._mouseLeaveHandler = (e: MouseEvent) => {
      this._mouseUpHandler(e);
    };


    this._mouseUpHandler = (e) => {
      const mousePosition: Position2D = this.getMousePosition(inputElement, e);
      this._handler.mouseUp(mousePosition, e);

      // end mouse session for drag and double click support
      if (this.notNull(this._currentMouseSession)) {
        // handle click
        if (new Date().valueOf() - this._currentMouseSession.downDate < 250 &&
          Math.abs(this._currentMouseSession.startPosition.x - mousePosition.x) < 10 &&
          Math.abs(this._currentMouseSession.startPosition.y - mousePosition.y) < 10) {
          this._handler.mouseClick(mousePosition, e);
        }

        // handle double click
        if (this.notNull(this._lastMouseSession) &&
          this._currentMouseSession.downDate - this._lastMouseSession.downDate < 500 &&
          Math.abs(this._lastMouseSession.startPosition.x - mousePosition.x) < 10 &&
          Math.abs(this._lastMouseSession.startPosition.y - mousePosition.y) < 10) {
          this._handler.mouseDoubleClick(mousePosition, e);
        }
        // handle drag
        inputElement.removeEventListener("mousemove", this._mouseDragHandler);
        inputElement.removeEventListener("mouseleave", this._mouseLeaveHandler);
        inputElement.addEventListener("mousemove", this._mouseMoveHandler);
        // reset mouse session
        this._lastMouseSession = this._currentMouseSession;
        this._currentMouseSession = null;
      }
    };


    inputElement.addEventListener("mousemove", this._mouseMoveHandler);
    inputElement.addEventListener("mousedown", this._mouseDownHandler);
    inputElement.addEventListener("mouseup", this._mouseUpHandler);


    document.body.addEventListener("keydown", (e) => {
      this._handler.keydown(e);
    });

    document.body.addEventListener('keyup',(e) => {
      this._handler.keyup(e);
    });

    document.body.addEventListener('keypress', (e) => {
      this._handler.keypress(e);
    });

  }

  private notNull(o: any) {
    return typeof o !== "undefined" && o != null;
  }

  private getTarget(e: EventTarget) {
    const target = e as HTMLElement;
    if ("classList" in target && target.classList.contains("overview")) {
      return null;
    } else if (!("classList" in target)){
      return null;
    }
    return target;
  }

  private getMousePosition(inputElement: HTMLElement, e: MouseEvent): Position2D {
    const x = ((e.clientX + window.pageXOffset) - offset(inputElement).left) * window.devicePixelRatio;
    const y = ((e.clientY + window.pageYOffset) - offset(inputElement).top) * window.devicePixelRatio;
    return new Position2D(x, y);
  }

  public clearRunning() {
    if (this._currentMouseSession != null) {
      const inputElement = this._inputElement;
      inputElement.removeEventListener("mousemove", this._mouseDragHandler);
      inputElement.addEventListener("mousemove", this._mouseMoveHandler);
      this._lastMouseSession = this._currentMouseSession;
      this._handler.mouseUp(this._currentMouseSession.currentPosition, null);
      this._currentMouseSession = null;
    }
  }

  public initScale() {
    viewerCrossBrowserWheel(this._inputElement, (e) => {
      this._handler.scroll(e);
    });
  }

  public updateOverview(overview: Rect, overviewScale: number, overviewBounding: Rect) {
    this._overviewRect = overview;
    this._overviewScale = overviewScale;
    this._overviewBounds = overviewBounding;
  }

  private createMouseSession(startPositionInputElement: Position2D, startPositionViewport: Position2D): MouseSession {
    return new MouseSession(startPositionInputElement, startPositionViewport, startPositionInputElement);
  }
}

class MouseSession {

  /**
   * Creates a new mouse session
   * @param startPosition the start position in the input element
   * @param startViewport the position of the viewport on mousedown
   * @param currentPosition the current position of the mouse (changes)
   */
  constructor(public startPosition: Position2D,
    public startViewport: Position2D,
    public currentPosition: Position2D) {
    this.downDate = new Date().getTime();
  }

  /**
   * The Date.valueOf the mouseDown occurred
   */
  public downDate: number;

}
