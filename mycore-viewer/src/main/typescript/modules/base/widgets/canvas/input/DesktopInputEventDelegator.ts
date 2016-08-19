/// <reference path="../../../Utils.ts" />
/// <reference path="../viewport/Viewport.ts" />
/// <reference path="../viewport/ZoomAnimation.ts" />
/// <reference path="MouseSession.ts" />
/// <reference path="DesktopInputEventHandler.ts" />

module mycore.viewer.widgets.canvas {

    export class DesktopInputEventDelegator {
        constructor(private _inputElement:JQuery, private _viewport:Viewport, private _handler:DesktopInputEventHandler) {
            this.initMove();
            this.initScale();
        }

        private _overviewRect: Rect;
        private _overviewBounds: Rect;
        private _overviewScale: number;

        private _lastMouseSession: MouseSession = null;
        private _currentMouseSession: MouseSession;

        private MMH;

        public initMove(): void {
            var inputElement = jQuery(this._inputElement[0]);

            var MOUSE_MOVE_HANDLER = this.MMH = (e:JQueryMouseEventObject) => {
                if(jQuery(e.target).hasClass("overview")){
                    return;
                }
                var x = ((e.clientX + window.pageXOffset) - inputElement.offset().left)* window.devicePixelRatio;;
                var y = ((e.clientY + window.pageYOffset) - inputElement.offset().top)* window.devicePixelRatio;;

                this._currentMouseSession.currentPositionInputElement = new Position2D(x, y);
                this._handler.mouseMove(this._currentMouseSession);
                e.preventDefault();
            };


            var MOUSE_DOWN_HANDLER = (e:JQueryMouseEventObject) => {
                if(jQuery(e.target).hasClass("overview")){
                    return;
                }
                var x = ((e.clientX + window.pageXOffset) - inputElement.offset().left)* window.devicePixelRatio;
                var y = ((e.clientY + window.pageYOffset) - inputElement.offset().top)* window.devicePixelRatio;

                this._currentMouseSession = this.createMouseSession(new Position2D(x, y), this._viewport.position.copy());
                this._handler.mouseDown(this._currentMouseSession);
                inputElement.bind("mousemove", MOUSE_MOVE_HANDLER);
                e.preventDefault();
            };

            var MOUSE_UP_HANDLER = (e) => {
                if(jQuery(e.target).hasClass("overview")){
                    return;
                }
                if (typeof this._currentMouseSession != "undefined" && this._currentMouseSession != null) {
                    this._lastMouseSession = this._currentMouseSession;
                    inputElement.unbind("mousemove", MOUSE_MOVE_HANDLER);
                    this._handler.mouseUp(this._currentMouseSession);
                    this._currentMouseSession = null;
                }
                e.preventDefault();
            };

            inputElement.mousedown(MOUSE_DOWN_HANDLER);
            inputElement.mouseup(MOUSE_UP_HANDLER);
            inputElement.mouseout(MOUSE_UP_HANDLER);

            var body = jQuery(document.body);
            body.keydown((e)=> {
                this._handler.keydown(e)
            });

            body.keyup((e)=> {
                this._handler.keyup(e)
            });

            body.keypress((e)=> {
                this._handler.keypress(e)
            });

        }

        public clearRunning() {
            if(this._currentMouseSession != null){
                this._lastMouseSession = this._currentMouseSession;
                this._inputElement[0].removeEventListener("mousemove", this.MMH, true);
                this._handler.mouseUp(this._currentMouseSession);
                this._currentMouseSession = null;
            }
        }

        public initScale() {
            var WHEEL_ZOOM_HANDLER = (e) => {
                this._handler.scroll(e);
            };

            viewerCrossBrowserWheel(this._inputElement[0], WHEEL_ZOOM_HANDLER);
        }


        public updateOverview(overview: Rect, overviewScale: number, overviewBounding: Rect) {
            this._overviewRect = overview;
            this._overviewScale = overviewScale;
            this._overviewBounds = overviewBounding;
        }

        private createMouseSession(startPositionInputElement: Position2D, startPositionViewport: Position2D): MouseSession {
            return new MouseSession(startPositionInputElement, startPositionViewport, startPositionInputElement, this._lastMouseSession);
        }
    }


}