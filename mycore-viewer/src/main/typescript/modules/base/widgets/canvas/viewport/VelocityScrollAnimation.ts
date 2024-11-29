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

import { Viewport } from "./Viewport";
import { MoveVector, Position2D } from "../../../Utils";
import { Animation } from "../Animation";

export class VelocityScrollAnimation implements Animation {

  constructor(private _viewport: Viewport, private _startVelocity: MoveVector) {
    this._currentVelocity = this._startVelocity.scale(1 / this._viewport.scale);
  }

  private _currentVelocity: MoveVector;
  private static MINIMUM_VELOCITY = 0.05;

  updateAnimation(elapsedTime: number): boolean {
    this._currentVelocity = this._currentVelocity.scale(1 / (1 + (0.01 * (elapsedTime / 3))));
    const isComplete = this._currentVelocity.x < VelocityScrollAnimation.MINIMUM_VELOCITY &&
      this._currentVelocity.x > -VelocityScrollAnimation.MINIMUM_VELOCITY &&
      this._currentVelocity.y < VelocityScrollAnimation.MINIMUM_VELOCITY &&
      this._currentVelocity.y > -VelocityScrollAnimation.MINIMUM_VELOCITY;

    if (!isComplete) {
      const oldPos = this._viewport.position;
      const newPosition = new Position2D(oldPos.x + (this._currentVelocity.x * elapsedTime), oldPos.y + (this._currentVelocity.y * elapsedTime));
      this._viewport.position = newPosition;
    }

    return isComplete;
  }
}

