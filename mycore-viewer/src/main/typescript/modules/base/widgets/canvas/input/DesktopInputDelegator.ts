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

/// <reference path="../../../Utils.ts" />
/// <reference path="../viewport/Viewport.ts" />
/// <reference path="../viewport/ZoomAnimation.ts" />
/// <reference path="DesktopInputListener.ts" />

namespace mycore.viewer.widgets.canvas {

    export class DesktopInputDelegator {
        constructor(private _inputElement: JQuery, private _viewport: Viewport, private _handler: DesktopInputListener) {
            this.initMove();
            this.initScale();
        }

        private _overviewRect: Rect;
        private _overviewBounds: Rect;
        private _overviewScale: number;

        private _lastMouseSession: MouseSession = null;
        private _currentMouseSession: MouseSession;

        private _mouseDownHandler: (e: JQueryMouseEventObject) => void;
        private _mouseUpHandler: (e: JQueryMouseEventObject) => void;
        private _mouseMoveHandler: (e: JQueryMouseEventObject) => void;
        private _mouseDragHandler: (e: JQueryMouseEventObject) => void;
        private _mouseLeaveHandler: (e: JQueryMouseEventObject) => void;

        public initMove(): void {
            let inputElement = jQuery(this._inputElement[ 0 ]);

            this._mouseMoveHandler = (e: JQueryMouseEventObject) => {
                let target = this.getTarget(e);
                if (target == null) {
                    return;
                }
                let mousePosition: Position2D = this.getMousePosition(inputElement, e);
                this._handler.mouseMove(mousePosition, e);
            };

            this._mouseDragHandler = (e: JQueryMouseEventObject) => {
                let target = this.getTarget(e);
                if (target == null) {
                    return;
                }
                let mousePosition: Position2D = this.getMousePosition(inputElement, e);
                this._handler.mouseDrag(mousePosition,
                    this._currentMouseSession.startPosition,
                    this._currentMouseSession.startViewport, e);
            };

            this._mouseDownHandler = (e: JQueryMouseEventObject) => {
                let target = this.getTarget(e);
                if (target == null) {
                    return;
                }
                let mousePosition: Position2D = this.getMousePosition(inputElement, e);
                this._handler.mouseDown(mousePosition, e);

                // start mouse session for drag and double click support
                this._currentMouseSession = this.createMouseSession(mousePosition, this._viewport.position.copy());
                inputElement.unbind("mousemove", this._mouseMoveHandler);
                inputElement.bind("mousemove", this._mouseDragHandler);
                inputElement.bind("mouseleave", this._mouseLeaveHandler);

            };

            this._mouseLeaveHandler = (e: JQueryMouseEventObject) => {
                this._mouseUpHandler(e);
            };


            this._mouseUpHandler = (e) => {
                let target = jQuery(e.target);
                var mousePosition: Position2D = this.getMousePosition(inputElement, e);
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
                    inputElement.unbind("mousemove", this._mouseDragHandler);
                    inputElement.unbind("mouseleave", this._mouseLeaveHandler);
                    inputElement.bind("mousemove", this._mouseMoveHandler);
                    // reset mouse session
                    this._lastMouseSession = this._currentMouseSession;
                    this._currentMouseSession = null;
                }
            };

            inputElement.bind("mousemove", this._mouseMoveHandler);
            inputElement.bind("mousedown", this._mouseDownHandler);
            inputElement.bind("mouseup", this._mouseUpHandler);

            let body = jQuery(document.body);
            body.keydown((e) => {
                this._handler.keydown(e);
            });

            body.keyup((e) => {
                this._handler.keyup(e);
            });

            body.keypress((e) => {
                this._handler.keypress(e);
            });

        }

        private notNull(o: any) {
            return typeof o !== "undefined" && o != null;
        }

        private getTarget(e: JQueryMouseEventObject) {
            let target = jQuery(e.target);
            if (target.hasClass("overview")) {
                return null;
            }
            return target;
        }

        private getMousePosition(inputElement: any, e: JQueryMouseEventObject): Position2D {
            let x = ((e.clientX + window.pageXOffset) - inputElement.offset().left) * window.devicePixelRatio;
            let y = ((e.clientY + window.pageYOffset) - inputElement.offset().top) * window.devicePixelRatio;
            return new Position2D(x, y);
        }

        public clearRunning() {
            if (this._currentMouseSession != null) {
                let inputElement = jQuery(this._inputElement[ 0 ]);
                inputElement.unbind("mousemove", this._mouseDragHandler);
                inputElement.bind("mousemove", this._mouseMoveHandler);
                this._lastMouseSession = this._currentMouseSession;
                this._handler.mouseUp(this._currentMouseSession.currentPosition, null);
                this._currentMouseSession = null;
            }
        }

        public initScale() {
            viewerCrossBrowserWheel(this._inputElement[ 0 ], (e) => {
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

}
