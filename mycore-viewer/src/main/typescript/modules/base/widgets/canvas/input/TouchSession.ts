/// <reference path="../../../Utils.ts" />
/// <reference path="TouchMove.ts" />

namespace mycore.viewer.widgets.canvas {
    export class TouchSession {
        constructor(public startTime: number,
            public startMiddle: Position2D,
            public startAngle: number,
            public startDistance: number,
            public canvasStartPosition: Position2D,
            public canvasStartScale: number,
            public canvasStartRotation: number,
            public currentMove: TouchMove,
            public lastMove: TouchMove,
            public lastSession: TouchSession,
            public touches: number,
            public touchLeft: boolean,
            public maxTouches: number) {
        }
    }
}