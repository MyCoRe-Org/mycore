/// <reference path="../../../Utils.ts" />
/// <reference path="Viewport.ts" />
/// <reference path="../Animation.ts" />

namespace mycore.viewer.widgets.canvas {
    export class ZoomAnimation implements Animation {

        constructor(private _viewport:mycore.viewer.widgets.canvas.Viewport, private _zoomScale:number, _position?:Position2D, private _duration:number = 300) {
            this._targetScale = _viewport.scale * this._zoomScale;
            this._startPosition = this._viewport.position;
            this._startScale = _viewport.scale;

            if (typeof _position != "undefined" && _position != null) {
                this._position = _position;
            } else {
                this._position = this._viewport.position.copy();
            }

            this._diff = new MoveVector(this._viewport.position.x - this._position.x, this._viewport.position.y-this._position.y).scale(this._startScale);
        }

        private _startScale:number = null;
        private _startPosition:Position2D = null;
        private _targetScale:number = null;
        private _totalElapsedTime:number = 0;
        private _position:Position2D;
        private _diff:MoveVector;

        updateAnimation(elapsedTime:number):boolean {
            this._totalElapsedTime += elapsedTime;
            var currentScale = (this._targetScale - this._startScale) * this._totalElapsedTime / this._duration + this._startScale;
            this._viewport.scale = currentScale;
            this._viewport.position = this._position.move(this._diff.scale(1/currentScale));

            var complete = this._totalElapsedTime >= (this._duration - elapsedTime);
            if (complete) {
                this._viewport.position = this._position.move(this._diff.scale(1/ this._targetScale));
                this._viewport.scale = this._targetScale;
            }

            return complete;
        }

        merge(additionalZoomScale:number):void {
            this._startScale = this._viewport.scale;
            this._targetScale *= additionalZoomScale;
            this._totalElapsedTime = 0;

        }

    }
}