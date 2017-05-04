/// <reference path="../../../Utils.ts" />
/// <reference path="Viewport.ts" />
/// <reference path="../Animation.ts" />

namespace mycore.viewer.widgets.canvas {
    export class VelocityScrollAnimation implements Animation {

        constructor(private _viewport: mycore.viewer.widgets.canvas.Viewport, private _startVelocity: MoveVector) {
            this._currentVelocity = this._startVelocity.scale(1 / this._viewport.scale);
        }

        private _currentVelocity: MoveVector;
        private static MINIMUM_VELOCITY = 0.05;

        updateAnimation(elapsedTime: number): boolean {
            this._currentVelocity = this._currentVelocity.scale(1 / (1 + (0.01 * (elapsedTime / 3))));
            var isComplete = this._currentVelocity.x < VelocityScrollAnimation.MINIMUM_VELOCITY &&
                this._currentVelocity.x > -VelocityScrollAnimation.MINIMUM_VELOCITY &&
                this._currentVelocity.y < VelocityScrollAnimation.MINIMUM_VELOCITY &&
                this._currentVelocity.y > -VelocityScrollAnimation.MINIMUM_VELOCITY;

            if (!isComplete) {
                var oldPos = this._viewport.position;
                var newPosition = new Position2D(oldPos.x + (this._currentVelocity.x * elapsedTime), oldPos.y + (this._currentVelocity.y * elapsedTime));
                this._viewport.position = newPosition;
            }

            return isComplete;
        }
    }
}