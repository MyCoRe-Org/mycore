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


import {ViewerEvent} from "./ViewerEvent";

export class ViewerEventManager {

    constructor() {
        this._callBackArray = new Array<any>();
    }

    private _callBackArray: Array<(e: ViewerEvent) => void>;

    public bind(callback: (e: ViewerEvent) => void) {
        this._callBackArray.push(callback);
    }

    public trigger(e: ViewerEvent) {
        for (const callback of this._callBackArray) {
            // if one handler throws a error it will be catched
            callback(e);
        }
    }

    public unbind(callback: (e: ViewerEvent) => void) {
        const index = this._callBackArray.lastIndexOf(callback);
        this._callBackArray.splice(index, 1);
    }

}

