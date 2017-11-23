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

namespace mycore.viewer.widgets.canvas {

    export interface DesktopInputListener {
        mouseDown(position: Position2D, e: JQueryMouseEventObject): void;
        mouseUp(position: Position2D, e: JQueryMouseEventObject): void;
        mouseClick(position: Position2D, e: JQueryMouseEventObject): void;
        mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void;
        mouseMove(position: Position2D, e: JQueryMouseEventObject): void;
        mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                  e: JQueryMouseEventObject): void;
        scroll(e: {
            deltaX: number;
            deltaY: number;
            orig: any;
            pos: Position2D;
            altKey?: boolean
        });
        keydown(e: JQueryKeyEventObject): void;
        keypress(e: JQueryKeyEventObject): void;
        keyup(e: JQueryKeyEventObject): void;
    }

    export abstract class DesktopInputAdapter implements DesktopInputListener {
        mouseDown(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseUp(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseDoubleClick(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseMove(position: Position2D, e: JQueryMouseEventObject): void {
        }

        mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
                  e: JQueryMouseEventObject): void {
        }

        scroll(e: {
            deltaX: number;
            deltaY: number;
            orig: any;
            pos: Position2D;
            altKey?: boolean
        }) {
        }

        keydown(e: JQueryKeyEventObject): void {
        }

        keypress(e: JQueryKeyEventObject): void {
        }

        keyup(e: JQueryKeyEventObject): void {
        }
    }

}
