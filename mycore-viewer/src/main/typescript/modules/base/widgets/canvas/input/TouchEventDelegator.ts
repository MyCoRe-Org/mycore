/// <reference path="../../../Utils.ts" />
/// <reference path="../viewport/Viewport.ts" />
/// <reference path="TouchEventHandler.ts" />
/// <reference path="VelocityCalculationQueue.ts" />
/// <reference path="TouchSession.ts" />
/// <reference path="TouchMove.ts" />
/// <reference path="TouchPolyfill.ts" />

module mycore.viewer.widgets.canvas {


    export class TouchInputDelegator {

        constructor(private _inputElement: JQuery, private _viewport: Viewport, private _handler: TouchEventHandler) {
            this.initTouch();
        }

        //that._viewport.position, that._viewport.scale, that._viewport.rotation
        public createTouchSession(startMiddle: Position2D, startAngle: number, startDistance: number, lastSession: TouchSession, canvasStartPosition: Position2D = this._viewport.position, canvasStartScale: number = this._viewport.scale, canvasStartRotation: number = this._viewport.rotation): TouchSession {
            var newSession = new TouchSession(new Date().valueOf(), startMiddle, startAngle, startDistance, canvasStartPosition, canvasStartScale, canvasStartRotation, null, null, lastSession, 0, false, 0);
            return newSession;
        }


        private msGestureTarget = null;
        private session:TouchSession = null;
        private lastSession: TouchSession = null;
        public listener = new Array<{
            surface: HTMLElement; type: string; fn: any
        }>();

        public initTouch(): void {
            var surface = this._inputElement[0];
            var that = this;

            var velocityCalculator = new VelocityCalculationQueue();

            var touchPoly = new TouchPolyfill(surface);

            var touchStartListener = function(e: any) {
                e.preventDefault();
                if (this.session == null) {
                    var angle = 0;
                    var touches = 0;
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

            var touchEndListener = function(e: any) {
                this.session.touches--;
                this.session.touchLeft = true;

                if ( this.session.touches == 0) {
                    that._handler.touchEnd( this.session);
                    this.session.lastSession = null;
                    this.lastSession =  this.session;
                    this.session = null;
                }
                e.preventDefault();
            };
            
            var touchMoveListener = function(e: any) {
                e.preventDefault();

                var currentMiddle = that.getMiddle(e.targetTouches);
                var positions = that.getPositions(e.targetTouches);
                var currentDistance = that.getDistance(e.targetTouches);
                var angle = 0;
                if ( this.session.touches == 2) {
                    angle = that.getAngle(e.targetTouches[0], e.targetTouches[1]);
                }

                var velocity: MoveVector = null;
                var delta: MoveVector = null;
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


                var move = new TouchMove(positions, currentMiddle, angle, currentDistance, new Date().valueOf(), velocity, delta);

                this.session.lastMove =  this.session.currentMove;
                this.session.currentMove = move;
                that._handler.touchMove( this.session);
            };

            this.addListener(surface, 'touchstart', touchStartListener);
            this.addListener(surface, 'touchend', touchEndListener);
            this.addListener(surface, 'touchmove', touchMoveListener);
            
            touchPoly.touchstart = <any>touchStartListener;
            touchPoly.touchmove = <any>touchMoveListener;
            touchPoly.touchend = <any>touchEndListener;

        }

        public clearRunning() {
            if(this.session != null){
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
            var positions: Array<Position2D> = new Array();

            for (var touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                var current = touches[touchIndex];
                positions.push(new Position2D(current.clientX * window.devicePixelRatio, current.clientY * window.devicePixelRatio));
            }

            return positions;
        }


        private getAngle(touch1, touch2): number {
            var y = touch2.pageY * window.devicePixelRatio - touch1.pageY * window.devicePixelRatio,
                x = touch2.pageX * window.devicePixelRatio - touch1.pageX * window.devicePixelRatio;
            return Math.atan2(y, x) * 180 / Math.PI;

        }

        private getMiddle(touches): Position2D {
            var xCollect = 0;
            var yCollect = 0;

            for (var touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                var current = touches[touchIndex];
                xCollect += current.clientX * window.devicePixelRatio;
                yCollect += current.clientY * window.devicePixelRatio;
            }

            var pos = new Position2D(xCollect / touches.length, yCollect / touches.length);
            return pos;
        }

        private getDistance(touches): number {
            var distCollect = 0;

            for (var touchIndex = 0; touchIndex < touches.length; touchIndex++) {
                var current = touches[touchIndex];

                var lastElem = touches[touchIndex - 1];
                if (typeof lastElem != "undefined" && lastElem != null) {
                    var distance = Math.sqrt(Math.pow(current.clientX - lastElem.clientX, 2) + Math.pow(current.clientY - lastElem.clientY, 2));
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