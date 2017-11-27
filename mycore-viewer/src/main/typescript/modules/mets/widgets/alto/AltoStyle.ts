
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

namespace mycore.viewer.widgets.alto {

    export class AltoStyle {
        // style of alto

        //Attribute der Elemente (die hier genannten MÜSSEN vorliegen)

        constructor(private _id: string,
                    private _fontFamily: string,
                    private _fontSize: number,
                    private _fontStyle: string
        ){
        }

        public getId(): string {
            return this._id;
        }

        public getFontFamily(): string {
            return this._fontFamily;
        }

        public getFontSize(): number {
            return this._fontSize;
        }

        public getFontStyle(): string {
            return this._fontStyle;
        }

    }

}
