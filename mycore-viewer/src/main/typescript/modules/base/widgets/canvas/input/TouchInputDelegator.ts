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
/// <reference path="TouchInputListener.ts" />
/// <reference path="VelocityCalculationQueue.ts" />
/// <reference path="TouchSession.ts" />
/// <reference path="TouchMove.ts" />
/// <reference path="TouchPolyfill.ts" />

namespace mycore.viewer.widgets.canvas {


    export class TouchInputDelegator {

        constructor(private _inputElement: JQuery, private _viewport: Viewport, private _handler: TouchInputListener) {
            this.initTouch();
        }

        public createTouchSession(startMiddle: Position2D, startAngle: number, startDistance: number, lastSession: TouchSession, canvasStartPosition: Position2D = this._viewport.position, canvasStartScale: number = this._viewport.scale, canvasStartRotation: number = this._viewport.rotation): TouchSession {
            return new TouchSession(new Date().valueOf(), startMiddle, startAngle, startDistance, canvasStartPosition, canvasStartScale, canvasStartRotation, null, null, lastSession, 0, false, 0);
        }

        private msGestureTarget = null;
        private session:TouchSession = null;
        private lastSession: TouchSession = null;
        public listener = new Array<{
            surface: HTMLElement; type: string; fn: any
        }>();

        public initTouch(): void {
            let surface = this._inputElement[0];
            let that = this;
            let velocityCalculator = new VelocityCalculationQueue();
            let touchPoly = new TouchPolyfill(surface);

            let touchStartListener = function(e: any) {
                e.preventDefault();
                if (this.session == null) {
                    let angle = 0;
                    let touches = 0;
                    velocityCalculator = new VelocityCalculationQueue();

                    this.session = that.createTouchSession(that.getMiddle(e.targetTouches), angle, that.getDistance(e.targetTouches), this.lastSession);
                    this.session.touches++;
                    this.session.maxTouches = this.session.touches;
                } else {
                    this.session.touches++;
                    if (this.session.touches > this.session.maxTouches) {
                        this.session.maxTouches = this.session.touches;
                    }
                    if ( this.session.touches == 2) {
                        this.session.startAngle = that.getAngle(e.targetTouches[0], e.targetTouches[1]);
                    }

                    this.session.startMiddle = that.getMiddle(e.targetTouches);
                    this.session.startDistance = that.getDistance(e.targetTouches);
                }

                that._handler.touchStart( this.session);
            };

            let touchEndListener = function(e: any) {
                this.session.touches--;
                this.session.touchLeft = true;

                if ( this.session.touches === 0) {
                    that._handler.touchEnd( this.session);
                    this.session.lastSession = null;
                    this.lastSession =  this.session;
                    this.session = null;
                }
                e.preventDefault();
            };

            let touchMoveListener = function(e: any) {
                e.preventDefault();
                let currentMiddle = that.getMiddle(e.targetTouches);
                let positions = that.getPositions(e.targetTouches);
                let currentDistance = that.getDistance(e.targetTouches);
                let angle = 0;
                if ( this.session.touches === 2) {
                    angle = that.getAngle(e.targetTouches[0], e.targetTouches[1]);
                }

                let velocity: MoveVector = null;
                let delta: MoveVector = null;
                if ( this.session.currentMove != null) {
                    velocityCalculator.add( this.session.currentMove);
                    velocity = velocityCalculator.getVelocity();
                    if ( this.session.lastMove != null) {
                        delta = new MoveVector( this.session.lastMove.middle.x -  this.session.currentMove.middle.x,  this.session.lastMove.middle.y -  this.session.currentMove.middle.y);
                    } else {
                        delta = new MoveVector(0, 0);
                    }

                } else {
                    velocity = new MoveVector(0, 0);
                    delta = new MoveVector(0, 0);
                }

                let move = new TouchMove(positions, currentMiddle, angle, currentDistance, new Date().valueOf(), velocity, delta);

                this.session.lastMove =  this.session.currentMove;
                this.session.currentMove = move;
                that._handler.touchMove( this.session);
            };

            this.addListener(surface, "touchstart", touchStartListener);
            this.addListener(surface, "touchend", touchEndListener);
            this.addListener(surface, "touchmove", touchMoveListener);

            touchPoly.touchstart = <any>touchStartListener;
            touchPoly.touchmove = <any>touchMoveListener;
            touchPoly.touchend = <any>touchEndListener;

        }

        public clearRunning() {
            if(this.session != null) {
                this._handler.touchEnd( this.session);
                this.session.lastSession = null;
                this.lastSession =  this.session;
                this.session = null;
            }
        }

        private addListener(surface: HTMLElement, type, fn) {
            surface.addEventListener(type, fn);
            this.listener.push({ surface: surface, type: type, fn: fn });
        }

        private getPositions(touches): Array<Position2D> {
            let positions: Array<Position2D> = new Array();
            for (let touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                let current = touches[touchIndex];
                positions.push(new Position2D(current.clientX * window.devicePixelRatio, current.clientY * window.devicePixelRatio));
            }
            return positions;
        }

        private getAngle(touch1, touch2): number {
            let y = touch2.pageY * window.devicePixelRatio - touch1.pageY * window.devicePixelRatio,
                x = touch2.pageX * window.devicePixelRatio - touch1.pageX * window.devicePixelRatio;
            return Math.atan2(y, x) * 180 / Math.PI;
        }

        private getMiddle(touches): Position2D {
            let xCollect = 0;
            let yCollect = 0;
            for (let touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                let current = touches[touchIndex];
                xCollect += current.clientX * window.devicePixelRatio;
                yCollect += current.clientY * window.devicePixelRatio;
            }
            return new Position2D(xCollect / touches.length, yCollect / touches.length);
        }

        private getDistance(touches): number {
            let distCollect = 0;
            for (let touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                let current = touches[touchIndex];
                let lastElem = touches[touchIndex - 1];
                if (typeof lastElem !== "undefined" && lastElem != null) {
                    let distance = Math.sqrt(Math.pow(current.clientX - lastElem.clientX, 2) + Math.pow(current.clientY - lastElem.clientY, 2));
                    distCollect += distance;
                }
            }
            return distCollect;
        }

        private getVelocity(deltaTime: number, delta: MoveVector) {
            return new MoveVector(Math.abs(delta.x / deltaTime) || 0, Math.abs(delta.y / deltaTime) || 0);
        }


        public delete() {
            this.listener.forEach((handler) => {
                handler.surface.removeEventListener(handler.type, handler.fn);
            });
        }
    }
}
