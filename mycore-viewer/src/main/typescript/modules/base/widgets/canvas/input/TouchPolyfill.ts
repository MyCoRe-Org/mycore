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

namespace mycore.viewer.widgets.canvas {
    export class TouchPolyfill {
        constructor(private _inputElement: Element) {

            this._inputElement.addEventListener("pointerdown", (e) => {
                if (e.pointerType == "touch") {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    e.stopPropagation();
                    this._updatePointer(e.pointerId.toString(10), new Position2D(e.offsetX, e.offsetY));
                    this._fireEvent("touchstart");
                }
            });

            this._inputElement.addEventListener("pointerup", (e) => {
                if (e.pointerType == "touch") {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    e.stopPropagation();
                    this._deletePointer(e.pointerId.toString(10));
                    this._fireEvent("touchend");
                }
            });

            this._inputElement.addEventListener("pointermove", (e) => {
                if (e.pointerType == "touch") {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    e.stopPropagation();
                    this._updatePointer(e.pointerId.toString(10), new Position2D(e.offsetX, e.offsetY));
                    this._fireEvent("touchmove");
                }
            });

            this._inputElement.addEventListener("pointercancel", (e) => {
                if (e.pointerType == "touch") {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                    e.stopPropagation();
                    this._deletePointer(e.pointerId.toString(10));
                    this._fireEvent("touchend");
                };
            });
        }

        private _idPointerMap = new MyCoReMap<string, Position2D>();
        private _handlerMap = new MyCoReMap<string, () => void>();

        private _deletePointer(id: string) {
            this._idPointerMap.remove(id);
        }

        private _updatePointer(id: string, pos: Position2D) {
            this._idPointerMap.set(id, pos);
        }

        private _fireEvent(eventName: string) {
            if (this._handlerMap.has(eventName)) {
                var handler = this._handlerMap.get(eventName);
                (<any>handler)(this._createTouchEvent());
            }
        }

        private _createTouchEvent() {
            return {
                preventDefault: () => { },
                targetTouches: this._createTouchesArray()
            };
        }

        private _createTouchesArray() {
            var arr = new Array<{ clientX: number; clientY: number,  pageX:number, pageY:number }>();
            this._idPointerMap.values.forEach((pos: Position2D) => arr.push({ clientX: pos.x, clientY: pos.y, pageX: pos.x, pageY: pos.y }));
            return arr;
        }

        public set touchstart(handler: () => void) {
            this._handlerMap.set("touchstart", handler);
        }

        public set touchmove(handler: () => void) {
            this._handlerMap.set("touchmove", handler);
        }

        public set touchend(handler: () => void) {
            this._handlerMap.set("touchend", handler);
        }

        public get touchstart() {
            return this._handlerMap.get("touchstart");
        }
        public get touchmove() {
            return this._handlerMap.get("touchmove");
        }

        public get touchend() {
            return this._handlerMap.get("touchend");
        }
    }


}
