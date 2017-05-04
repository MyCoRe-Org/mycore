/// <reference path="../../../Utils.ts" />
/// <reference path="TouchMove.ts" />

namespace mycore.viewer.widgets.canvas {
    export class VelocityCalculationQueue {


        constructor(private _maxElements = 20, private _maxTime = 300) {
            this._maxElementsOrig = this._maxElements;
            this._values = new Array();
        }

        private _values: Array<TouchMove>;
        private _maxElementsOrig: number;

        public add(move: TouchMove) {
            if (this._values.length > 0) {
                var last = this._values.pop();
                this._values.push(last);

                if (move.time - last.time >= 5) {
                    this._values.push(move);
                }

                var arr = this._values.reverse();
                arr.length = Math.min(arr.length, this._maxElements);
                this._values = arr.reverse();
            } else {
                this._values.push(move);
            }
        }

        public getVelocity(): MoveVector {
            var newest: TouchMove = this._values.pop();
            this._values.push(newest);

            if (this._values.length == 0) {
                return new MoveVector(0, 0);
            }

            var oldest: TouchMove = null;

            for (var i in this._values) {
                var current = this._values[i];
                var isOlderThenMaxTime = Math.abs(current.time - newest.time) > this._maxTime;
                if (!isOlderThenMaxTime) {
                    oldest = current;
                    break;
                }
            }

            var deltaTime = newest.time - oldest.time;
            var delta = new MoveVector(oldest.middle.x - newest.middle.x, oldest.middle.y - newest.middle.y);

            return new MoveVector((delta.x / deltaTime) || 0, (delta.y / deltaTime) || 0);
        }
    }

}