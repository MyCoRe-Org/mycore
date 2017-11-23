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
