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


import {Position2D} from "../../../Utils";

export interface DesktopInputListener {
    keydown(e: JQuery.KeyboardEventBase): void;

    keypress(e: JQuery.KeyboardEventBase): void;

    keyup(e: JQuery.KeyboardEventBase): void;

    mouseClick(position: Position2D, e: JQuery.MouseEventBase): void;

    mouseDoubleClick(position: Position2D, e: JQuery.MouseEventBase): void;

    mouseDown(position: Position2D, e: JQuery.MouseEventBase): void;

    mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
              e: JQuery.MouseEventBase): void;

    mouseMove(position: Position2D, e: JQuery.MouseEventBase): void;

    mouseUp(position: Position2D, e: JQuery.MouseEventBase): void;

    scroll(e: {
        deltaX: number;
        deltaY: number;
        orig: any;
        pos: Position2D;
        altKey?: boolean
    }):void;
}

export abstract class DesktopInputAdapter implements DesktopInputListener {

    mouseDown(position: Position2D, e: JQuery.MouseEventBase): void {
    }

    mouseUp(position: Position2D, e: JQuery.MouseEventBase): void {
    }

    mouseClick(position: Position2D, e: JQuery.MouseEventBase): void {
    }

    mouseDoubleClick(position: Position2D, e: JQuery.MouseEventBase): void {
    }

    mouseMove(position: Position2D, e: JQuery.MouseEventBase): void {
    }

    mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
              e: JQuery.MouseEventBase): void {
    }

    scroll(e: {
        deltaX: number;
        deltaY: number;
        orig: any;
        pos: Position2D;
        altKey?: boolean
    }) {
    }

    keydown(e: JQuery.KeyboardEventBase): void {
    }

    keypress(e: JQuery.KeyboardEventBase): void {
    }

    keyup(e: JQuery.KeyboardEventBase): void {
    }
}

