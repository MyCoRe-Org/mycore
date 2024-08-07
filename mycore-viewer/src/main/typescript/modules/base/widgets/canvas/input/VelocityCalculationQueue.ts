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


import {MoveVector} from "../../../Utils";
import {TouchMove} from "./TouchMove";

export class VelocityCalculationQueue {


    constructor(private _maxElements = 20, private _maxTime = 300) {
        this._maxElementsOrig = this._maxElements;
        this._values = [];
    }

    private _values: Array<TouchMove>;
    private _maxElementsOrig: number;

    public add(move: TouchMove) {
        if (this._values.length > 0) {
            const last = this._values.pop();
            this._values.push(last);

            if (move.time - last.time >= 5) {
                this._values.push(move);
            }

            const arr = this._values.reverse();
            arr.length = Math.min(arr.length, this._maxElements);
            this._values = arr.reverse();
        } else {
            this._values.push(move);
        }
    }

    public getVelocity(): MoveVector {
        const newest: TouchMove = this._values.pop();
        this._values.push(newest);

        if (this._values.length == 0) {
            return new MoveVector(0, 0);
        }

        let oldest: TouchMove = null;

        for (const current of this._values) {
            const isOlderThenMaxTime = Math.abs(current.time - newest.time) > this._maxTime;
            if (!isOlderThenMaxTime) {
                oldest = current;
                break;
            }
        }

        const deltaTime = newest.time - oldest.time;
        const delta = new MoveVector(oldest.middle.x - newest.middle.x, oldest.middle.y - newest.middle.y);

        return new MoveVector((delta.x / deltaTime) || 0, (delta.y / deltaTime) || 0);
    }
}


