/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


import { Position2D } from "../../../Utils";

export interface DesktopInputListener {
  keydown(e: KeyboardEvent): void;

  keypress(e: KeyboardEvent): void;

  keyup(e: KeyboardEvent): void;

  mouseClick(position: Position2D, e: MouseEvent): void;

  mouseDoubleClick(position: Position2D, e: MouseEvent): void;

  mouseDown(position: Position2D, e: MouseEvent): void;

  mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
    e: MouseEvent): void;

  mouseMove(position: Position2D, e: MouseEvent): void;

  mouseUp(position: Position2D, e: MouseEvent): void;

  scroll(e: {
    deltaX: number;
    deltaY: number;
    orig: any;
    pos: Position2D;
    altKey?: boolean
  }): void;
}

export abstract class DesktopInputAdapter implements DesktopInputListener {

  mouseDown(position: Position2D, e: MouseEvent): void {
  }

  mouseUp(position: Position2D, e:MouseEvent): void {
  }

  mouseClick(position: Position2D, e: MouseEvent): void {
  }

  mouseDoubleClick(position: Position2D, e: MouseEvent): void {
  }

  mouseMove(position: Position2D, e: MouseEvent): void {
  }

  mouseDrag(currentPosition: Position2D, startPosition: Position2D, startViewport: Position2D,
    e: MouseEvent): void {
  }

  scroll(e: {
    deltaX: number;
    deltaY: number;
    orig: any;
    pos: Position2D;
    altKey?: boolean
  }) {
  }

  keydown(e: KeyboardEvent): void {
  }

  keypress(e: KeyboardEvent): void {
  }

  keyup(e: KeyboardEvent): void {
  }
}

