/// <reference path="../../../Utils.ts" />

namespace mycore.viewer.widgets.canvas {
    export class TouchMove {
        constructor(
            public positions: Array<Position2D>,
            public middle: Position2D,
            public angle: number,
            public distance: number,
            public time: number,
            public velocity: MoveVector,
            public delta: MoveVector
        ) {
        }
    }
}
